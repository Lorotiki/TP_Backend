package com.tpi.portfolio.service;

import com.tpi.portfolio.dto.DepositoRequest;
import com.tpi.portfolio.dto.DepositoResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class PortafolioService {

    private final CuentaRepository cuentaRepository;
    private final PosicionRepository posicionRepository;
    private final MovimientoDineroRepository movimientoDineroRepository;

    public PortafolioService(CuentaRepository cuentaRepository,
                             PosicionRepository posicionRepository,
                             MovimientoDineroRepository movimientoDineroRepository) {
        this.cuentaRepository = cuentaRepository;
        this.posicionRepository = posicionRepository;
        this.movimientoDineroRepository = movimientoDineroRepository;
    }

    @Transactional
    public PortafolioResponse getPortafolio(String userId) {
        var cuenta = getOrCreateCuenta(userId);
        var posiciones = posicionRepository.findByAccountId(cuenta.getId()).stream()
                .map(posicion -> new PosicionResponse(posicion.getSymbol(), posicion.getQuantity(), posicion.getAvgPriceArs()))
                .toList();
        return new PortafolioResponse(cuenta.getUserId(), cuenta.getBalanceArs(), posiciones);
    }

    @Transactional
    public DepositoResponse deposito(String userId, DepositoRequest request) {
        var cuenta = getOrCreateCuenta(userId);
        cuenta.setBalanceArs(cuenta.getBalanceArs().add(redondeo2Cifras(request.amountArs())));
        cuentaRepository.save(cuenta);

        var movimiento = new MovimientoDinero();
        movimiento.setAccountId(cuenta.getId());
        movimiento.setType("DEPOSIT");
        movimiento.setAmountArs(redondeo2Cifras(request.amountArs()));
        movimiento.setReferenceId(request.referenceId());
        movimientoDineroRepository.save(movimiento);

        return new DepositoResponse(movimiento.getId(), cuenta.getBalanceArs());
    }

    @Transactional
    public PortafolioResponse aplicarTrade(String userId, TradeAdjustmentRequest request) {
        var cuenta = getOrCreateCuenta(userId);
        var side = normalizar(request.side());
        var simbolo = request.symbol().trim().toUpperCase(Locale.ROOT);
        var cantidad = request.quantity().setScale(4, RoundingMode.HALF_UP);
        var precio = redondeo4Cifras(request.priceArs());
        var totalCuenta = redondeo2Cifras(precio.multiply(cantidad));

        //todo revisar
        if ("BUY".equals(side)) {
            if (cuenta.getBalanceArs().compareTo(totalCuenta) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para concretar la compra");
            }
            cuenta.setBalanceArs(cuenta.getBalanceArs().subtract(totalCuenta));
            var posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), simbolo)
                    .orElseGet(() -> createPosition(cuenta.getId(), simbolo));
            var cantidadActual = defaultCantidad(posicion.getQuantity());
            var cuentaActual = cantidadActual.multiply(posicion.getAvgPriceArs());
            var nuevaCantidad = cantidadActual.add(cantidad);
            var nuevaCuenta = cuentaActual.add(precio.multiply(cantidad));
            posicion.setQuantity(nuevaCantidad);
            posicion.setAvgPriceArs(nuevaCuenta.divide(nuevaCantidad, 4, RoundingMode.HALF_UP));
            posicionRepository.save(posicion);
            saveMovimiento(cuenta.getId(), "BUY_DEBIT", totalCuenta, request.referenceId());
        } else if ("SELL".equals(side)) {
            var posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), simbolo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no posee tenencias para el símbolo " + simbolo));
            if (posicion.getQuantity().compareTo(cantidad) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad insuficiente para vender");
            }
            posicion.setQuantity(posicion.getQuantity().subtract(cantidad));
            if (posicion.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                posicionRepository.delete(posicion);
            } else {
                posicionRepository.save(posicion);
            }
            cuenta.setBalanceArs(cuenta.getBalanceArs().add(totalCuenta));
            saveMovimiento(cuenta.getId(), "SELL_CREDIT", totalCuenta, request.referenceId());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lado de operación inválido");
        }

        cuentaRepository.save(cuenta);
        return getPortafolio(userId);
    }

    private Posicion createPosition(java.util.UUID accountId, String symbol) {
        var posicion = new Posicion();
        posicion.setAccountId(accountId);
        posicion.setSymbol(symbol);
        posicion.setQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        posicion.setAvgPriceArs(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        return posicion;
    }

    private BigDecimal defaultCantidad(BigDecimal cantidad) {
        return cantidad == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : cantidad;
    }

    private void saveMovimiento(java.util.UUID accountId, String type, BigDecimal amount, String referenceId) {
        var movimiento = new MovimientoDinero();
        movimiento.setAccountId(accountId);
        movimiento.setType(type);
        movimiento.setAmountArs(redondeo2Cifras(amount));
        movimiento.setReferenceId(referenceId);
        movimientoDineroRepository.save(movimiento);
    }

    private Cuenta getOrCreateCuenta(String userId) {
        return cuentaRepository.findByUserId(userId)
                .orElseGet(() -> cuentaRepository.save(createCuenta(userId)));
    }

    private Cuenta createCuenta(String userId) {
        var account = new Cuenta();
        account.setUserId(userId);
        account.setBalanceArs(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return account;
    }

    private String normalizar(String side) {
        return side == null ? "" : side.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal redondeo2Cifras(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal redondeo4Cifras(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}

