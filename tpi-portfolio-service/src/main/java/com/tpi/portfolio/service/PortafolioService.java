package com.tpi.portfolio.service;

import com.tpi.portfolio.dto.DepositoRequest;
import com.tpi.portfolio.dto.DepositoResponse;
import com.tpi.portfolio.dto.HistorialEventoClienteRequest;
import com.tpi.portfolio.dto.PortafolioResponse;
import com.tpi.portfolio.dto.PosicionResponse;
import com.tpi.portfolio.dto.TradeAdjustmentRequest;
import com.tpi.portfolio.entity.Cuenta;
import com.tpi.portfolio.entity.MovimientoDinero;
import com.tpi.portfolio.entity.Posicion;
import com.tpi.portfolio.repository.CuentaRepository;
import com.tpi.portfolio.repository.MovimientoDineroRepository;
import com.tpi.portfolio.repository.PosicionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PortafolioService {

    private final CuentaRepository cuentaRepository;
    private final PosicionRepository posicionRepository;
    private final MovimientoDineroRepository movimientoDineroRepository;
    private final RestTemplate restTemplate;
    private final String historialUrl;

    public PortafolioService(CuentaRepository cuentaRepository,
                             PosicionRepository posicionRepository,
                             MovimientoDineroRepository movimientoDineroRepository,
                             RestTemplate restTemplate,
                             @Value("${clients.history.base-url}") String historialUrl) {
        this.cuentaRepository = cuentaRepository;
        this.posicionRepository = posicionRepository;
        this.movimientoDineroRepository = movimientoDineroRepository;
        this.restTemplate = restTemplate;
        this.historialUrl = historialUrl;
    }

    @Transactional
    public PortafolioResponse getPortafolio(String userId) {
        log.info("Buscando o creando portafolio para el usuario: {}", userId);
        Cuenta cuenta = findOrCreateAccount(userId);

        List<Posicion> posiciones = posicionRepository.findByAccountId(cuenta.getId());

        List<PosicionResponse> posicionResponses = posiciones.stream()
                .map(p -> new PosicionResponse(p.getSymbol(), p.getQuantity(), p.getAvgPriceArs()))
                .collect(Collectors.toList());

        log.info("Portafolio encontrado para el usuario: {}. Saldo: {}, Posiciones: {}", userId, cuenta.getBalanceArs(), posicionResponses.size());
        return new PortafolioResponse(cuenta.getUserId(), cuenta.getBalanceArs(), posicionResponses);
    }

    @Transactional
    public DepositoResponse deposito(String userId, DepositoRequest request) {
        log.info("Procesando depósito para usuario: {}, monto: {}", userId, request.amountArs());
        Cuenta cuenta = findOrCreateAccount(userId);

        if (request.amountArs().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del depósito debe ser positivo.");
        }

        cuenta.setBalanceArs(cuenta.getBalanceArs().add(request.amountArs()));
        cuentaRepository.save(cuenta);

        MovimientoDinero movement = new MovimientoDinero();
        movement.setAccountId(cuenta.getId());
        movement.setType("DEPOSIT");
        movement.setAmountArs(request.amountArs());
        movement.setReferenceId(request.referenceId());
        movement = movimientoDineroRepository.save(movement);

        registrarDepositoEnHistorial(userId, request, movement.getId().toString(), cuenta.getBalanceArs());

        log.info("Depósito completado para usuario: {}. Nuevo saldo: {}", userId, cuenta.getBalanceArs());
        return new DepositoResponse(movement.getId(), cuenta.getBalanceArs());
    }

    @Transactional
    public PortafolioResponse aplicarTrade(String userId, TradeAdjustmentRequest request) {
        log.info("Aplicando trade para usuario {}: {} {} de {} @ {}", userId, request.side(), request.quantity(), request.symbol(), request.priceArs());

        Cuenta cuenta = findOrCreateAccount(userId);
        BigDecimal totalAmount = request.priceArs().multiply(request.quantity());

        if ("BUY".equalsIgnoreCase(request.side())) {
            // Lógica de Compra
            if (cuenta.getBalanceArs().compareTo(totalAmount) < 0) {
                log.error("Saldo insuficiente para el usuario {}. Necesario: {}, Disponible: {}", userId, totalAmount, cuenta.getBalanceArs());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para realizar la compra.");
            }
            cuenta.setBalanceArs(cuenta.getBalanceArs().subtract(totalAmount));

            Posicion posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), request.symbol())
                    .orElseGet(() -> {
                        log.info("Creando nueva posición para el símbolo {} en la cuenta {}", request.symbol(), cuenta.getId());
                        Posicion nuevaPosicion = new Posicion();
                        nuevaPosicion.setAccountId(cuenta.getId());
                        nuevaPosicion.setSymbol(request.symbol());
                        nuevaPosicion.setQuantity(BigDecimal.ZERO);
                        nuevaPosicion.setAvgPriceArs(BigDecimal.ZERO);
                        return nuevaPosicion;
                    });

            // Recalcular precio promedio ponderado
            BigDecimal newQuantity = posicion.getQuantity().add(request.quantity());
            BigDecimal newTotalValue = (posicion.getAvgPriceArs().multiply(posicion.getQuantity())).add(totalAmount);
            BigDecimal newAvgPrice = newTotalValue.divide(newQuantity, 4, RoundingMode.HALF_UP);

            posicion.setQuantity(newQuantity);
            posicion.setAvgPriceArs(newAvgPrice);
            posicionRepository.save(posicion);

        } else if ("SELL".equalsIgnoreCase(request.side())) {
            // Lógica de Venta
            Posicion posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), request.symbol())
                    .orElseThrow(() -> {
                        log.error("El usuario {} intentó vender el símbolo {} pero no tiene posición.", userId, request.symbol());
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "No posee acciones del símbolo " + request.symbol() + " para vender.");
                    });

            if (posicion.getQuantity().compareTo(request.quantity()) < 0) {
                log.error("Cantidad de acciones insuficiente para el usuario {}. A vender: {}, Disponible: {}", userId, request.quantity(), posicion.getQuantity());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad de acciones insuficiente para vender.");
            }

            posicion.setQuantity(posicion.getQuantity().subtract(request.quantity()));
            cuenta.setBalanceArs(cuenta.getBalanceArs().add(totalAmount));

            if (posicion.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                log.info("La cantidad de la posición para el símbolo {} llegó a cero. Eliminando posición.", request.symbol());
                posicionRepository.delete(posicion);
            } else {
                posicionRepository.save(posicion);
            }
        }

        cuentaRepository.save(cuenta);
        log.info("Trade aplicado exitosamente para el usuario {}. Nuevo saldo: {}", userId, cuenta.getBalanceArs());
        return getPortafolio(userId);
    }

    /**
     * Busca una cuenta por userId. Si no la encuentra, crea una nueva con saldo cero.
     * Este método es clave para evitar NullPointerExceptions.
     */
    private Cuenta findOrCreateAccount(String userId) {
        return cuentaRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("No se encontró cuenta para el usuario: {}. Creando una nueva.", userId);
                    Cuenta nuevaCuenta = new Cuenta();
                    nuevaCuenta.setUserId(userId);
                    nuevaCuenta.setBalanceArs(BigDecimal.ZERO);
                    return cuentaRepository.save(nuevaCuenta);
                });
    }

    private void registrarDepositoEnHistorial(String userId, DepositoRequest request, String movementId, BigDecimal nuevoSaldo) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amountArs", request.amountArs());
            payload.put("referenceId", request.referenceId());
            payload.put("movementId", movementId);
            payload.put("balanceArs", nuevoSaldo);
            payload.put("mensaje", "Depósito acreditado");

            var eventId = UUID.randomUUID();
            var historialRequest = new HistorialEventoClienteRequest(
                    eventId,
                    "DEPOSIT_RECEIVED",
                    userId,
                    null,
                    eventId,
                    null,
                    payload
            );

            restTemplate.postForObject(historialUrl + "/events", historialRequest, Void.class);
            log.debug("Evento de depósito registrado en historial: usuario={}, movementId={}", userId, movementId);
        } catch (Exception e) {
            log.warn("Error registrando depósito en historial: {}", e.getMessage());
        }
    }
}
