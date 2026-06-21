package com.tpi.orders.service;

import com.tpi.orders.dto.*;
import com.tpi.orders.entity.OrdenEntity;
import com.tpi.orders.repository.OrdenFillRepository;
import com.tpi.orders.repository.OrdenRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final OrdenFillRepository ordenFillRepository;
    private final RestTemplate restTemplate;
    private final String mercadoUrl;
    private final String portafolioUrl;
    private final String historialUrl;

    public OrdenService(OrdenRepository ordenRepository,
                        OrdenFillRepository ordenFillRepository,
                        RestTemplate restTemplate,
                        @Value("${clients.market-data.base-url}") String mercadoUrl,
                        @Value("${clients.portfolio.base-url}") String portafolioUrl,
                        @Value("${clients.history.base-url}") String historialUrl) {
        this.ordenRepository = ordenRepository;
        this.ordenFillRepository = ordenFillRepository;
        this.restTemplate = restTemplate;
        this.mercadoUrl = mercadoUrl;
        this.portafolioUrl = portafolioUrl;
        this.historialUrl = historialUrl;
    }

    @Transactional
    public OrdenResponse vender(OrdenRequest request) {
        log.info("▶️ Procesando orden de venta: usuarioID={}, simbolo={}, cantidad={}, precioLimite={}",
                request.userId(), request.symbol(), request.quantity(), request.priceLimit());
        try {
            var orden = createOrden(request, "SELL");
            validaDisponibilidadAcciones(orden);
            orden.setStatus("PENDING");
            ordenRepository.save(orden);

            log.info("✅ Orden de venta registrada: orderId={}, status={}",
                    orden.getId(), orden.getStatus());

            recordHistorial("SELL_ORDER_CREATED",
                            orden, //orden
                            orden.getQuantity(), //cantidad
                            orden.getRemainingQuantity(), //cantidadRemaninete
                            orden.getLimitPrice(), //precio limite
                            "Orden de venta registrada");

            return toResponse(orden, BigDecimal.ZERO, orden.getRemainingQuantity(),
                    orden.getLimitPrice(), "Orden de venta registrada");
        } catch (Exception e) {
            log.error("❌ Error procesando orden de venta: usuarioID={}, simbolo={}",
                    request.userId(), request.symbol(), e);
            throw e;
        }
    }

    @Transactional
    public OrdenResponse comprar(OrdenRequest request) {
        log.info("▶️ Procesando orden de compra: usuario={}, simbolo={}, cantidad={}, precioLimite={}",
                request.userId(), request.symbol(), request.quantity(), request.priceLimit());

        try {
            // Consultar cotización
            var cotizacionMercado = obtenerCotizacion(request.symbol());
            log.debug("✓ Cotización obtenida: simbolo={}, precio={}, moneda={}",
                    cotizacionMercado.symbol(), cotizacionMercado.price(), cotizacionMercado.currency());

            var orden = createOrden(request, "BUY");
            log.debug("✓ Orden creada: orderId={}", orden.getId());

            // Validar balance
            validaSaldoUsuario(orden, cotizacionMercado.price());
            log.debug("✓ Balance validado: saldoRequerido={} ARS",
                    orden.getQuantity().multiply(orden.getLimitPrice()));

            ordenRepository.save(orden);
            log.debug("✓ Orden persistida en BD");

            // Matching
            var vendedoresDisponibles = ordenRepository.findCoincidenciasVentas(orden.getSymbol(), "SELL");
            log.debug("✓ Encontradas {} órdenes de venta disponibles", vendedoresDisponibles.size());

            var cantidades = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            for (var vendedor : vendedoresDisponibles) {
                if (orden.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("✓ Cantidad completa satisfecha, saliendo del loop");
                    break;
                }

                if (vendedor.getUserId().equals(orden.getUserId())) {
                    log.debug("⊘ Saltando venta del mismo usuario: usuarioID={}", orden.getUserId());
                    continue;
                }

                if (vendedor.getLimitPrice().compareTo(orden.getLimitPrice()) > 0) {
                    log.debug("⊘ Precio no coincide: vendedor.precio={} > comprador.precio={}",
                            vendedor.getLimitPrice(), orden.getLimitPrice());
                    continue;
                }

                var cantidad = vendedor.getRemainingQuantity().min(orden.getRemainingQuantity())
                        .setScale(4, RoundingMode.HALF_UP);

                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("⊘ Cantidad inválida: cantidad={}", cantidad);
                    continue;
                }

                log.info("✓ MATCH encontrado: comprador={}, vendedor={}, cantidad={}, precio={}",
                        orden.getUserId(), vendedor.getUserId(), cantidad, vendedor.getLimitPrice());

                var precioVenta = vendedor.getLimitPrice().setScale(4, RoundingMode.HALF_UP);

                ejecutarCompraXPortafolio(orden.getUserId(), "BUY", orden.getSymbol(), cantidad,
                        precioVenta, orden.getId().toString());
                ejecutarCompraXPortafolio(vendedor.getUserId(), "SELL", vendedor.getSymbol(), cantidad,
                        precioVenta, vendedor.getId().toString());

                //to-do deberia hacerse en el evento q ejecuta la compra y la venta anteriores

                vendedor.setRemainingQuantity(vendedor.getRemainingQuantity().subtract(cantidad));
                vendedor.setStatus(resolveStatus(vendedor.getRemainingQuantity(), vendedor.getQuantity()));
                orden.setRemainingQuantity(orden.getRemainingQuantity().subtract(cantidad));
                orden.setStatus(resolveStatus(orden.getRemainingQuantity(), orden.getQuantity()));

                cantidades = cantidades.add(cantidad);
            }

            ordenRepository.saveAll(List.of(orden));
            recordHistorial("BUY_ORDER_EXECUTED", orden, cantidades, orden.getRemainingQuantity(),
                    orden.getLimitPrice(), "Orden de compra ejecutada");

            log.info("✅ Orden completada: ordenID={}, cantidadCoincidida={}, cantidadRestante={}, estado={}",
                    orden.getId(), cantidades, orden.getRemainingQuantity(), orden.getStatus());

            return toResponse(orden, cantidades, orden.getRemainingQuantity(),
                    orden.getLimitPrice(), "Orden de compra ejecutada");
        } catch (Exception e) {
            log.error("❌ Error procesando orden de compra: usuarioID={}, simbolo={}",
                    request.userId(), request.symbol(), e);
            throw e;
        }
    }

    public List<OrdenResponse> getOrdenesByUsuario(String userId) {
        return ordenRepository.findByUsuarioIdOrdenByCreatedAtDesc(userId).stream()
                .map(orden -> toResponse(orden,
                        orden.getQuantity().subtract(orden.getRemainingQuantity()),
                        orden.getRemainingQuantity(),
                        orden.getLimitPrice(),
                        "Consulta de órdenes"))
                .toList();
    }

    private void ejecutarCompraXPortafolio(String userId, String side, String symbol, BigDecimal quantity,
                                           BigDecimal priceArs, String referenceId) {
        var request = new PortafolioTradeClienteRequest(side, symbol, quantity, priceArs, referenceId);
        try {
            restTemplate.postForEntity(portafolioUrl + "/users/" + userId + "/portfolio/trades", request,
                    Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo actualizar portfolio-service", e);
        }
    }

    /**
     * Valida que el usuario tenga saldo suficiente para la compra
     */
    private void validaSaldoUsuario(OrdenEntity order, BigDecimal marketPrice) {
        log.debug("Validando balance para compra: userId={}, qty={}, limitPrice={}",
                order.getUserId(), order.getQuantity(), order.getLimitPrice());

        // Consultar portafolio del usuario
        try {
            PortafolioClienteResponse portafolio = restTemplate.getForObject(
                    portafolioUrl + "/users/" + order.getUserId() + "/portfolio",
                    PortafolioClienteResponse.class);

            if (portafolio == null) {
                log.warn("⚠️ Portafolio no encontrado para userId: {}", order.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portafolio no encontrado para el usuario");
            }

            // Calcular monto requerido
            BigDecimal montoRequerido = order.getQuantity()
                    .multiply(order.getLimitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Balance requerido: {} ARS, Balance disponible: {} ARS",
                    montoRequerido, portafolio.balanceArs());

            // Validar saldo
            if (portafolio.balanceArs().compareTo(montoRequerido) < 0) {
                log.warn("❌ Saldo insuficiente: usuario={}, requerido={}, disponible={}",
                        order.getUserId(), montoRequerido, portafolio.balanceArs());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Saldo insuficiente. Requerido: %s ARS, Disponible: %s ARS",
                                montoRequerido, portafolio.balanceArs()));
            }

            log.debug("✓ Balance validado correctamente");

        } catch (Exception e) {
            log.error("Error validando balance: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar saldo del usuario");
        }
    }

    private PortafolioClienteResponse consultarPortafolio(String usuarioId) {
        try {
            return restTemplate.getForObject(portafolioUrl + "/users/" + usuarioId + "/portfolio",
                    PortafolioClienteResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar portfolio-service", e);
        }
    }

    private CotizacionClienteResponse obtenerCotizacion(String symbol) {
        try {
            return restTemplate.getForObject(mercadoUrl + "/quotes/" + symbol, CotizacionClienteResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar market-data-service", e);
        }
    }

    private void recordHistorial(String eventType,
                                 OrdenEntity order,
                                 BigDecimal matchedQuantity,
                                 BigDecimal remainingQuantity,
                                 BigDecimal priceArs,
                                 String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", order.getSymbol());
        payload.put("side", order.getSide());
        payload.put("quantity", order.getQuantity());
        payload.put("matchedQuantity", matchedQuantity);
        payload.put("remainingQuantity", remainingQuantity);
        payload.put("priceArs", priceArs);
        payload.put("amountArs", priceArs.multiply(matchedQuantity).setScale(2, RoundingMode.HALF_UP));
        payload.put("message", message);

        var request = new HistorialEventosClienteRequest(UUID.randomUUID(), eventType, order.getUserId(), order.getId(),
                UUID.randomUUID(), null, payload);
        try {
            restTemplate.postForEntity(historialUrl + "/events", request, Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo registrar el historial", e);
        }
    }

    private OrdenEntity createOrden(OrdenRequest request, String side) {
        var order = new OrdenEntity();
        order.setUserId(request.userId());
        order.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        order.setSide(side);
        order.setQuantity(request.quantity().setScale(4, RoundingMode.HALF_UP));
        order.setRemainingQuantity(request.quantity().setScale(4, RoundingMode.HALF_UP));
        order.setLimitPrice(request.priceLimit().setScale(4, RoundingMode.HALF_UP));
        order.setStatus("PENDING");
        return order;
    }

    private String resolveStatus(BigDecimal remainingQuantity, BigDecimal totalQuantity) {
        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "FILLED";
        }
        if (remainingQuantity.compareTo(totalQuantity) < 0) {
            return "PARTIALLY_FILLED";
        }
        return "PENDING";
    }

    private OrdenResponse toResponse(OrdenEntity order,
                                     BigDecimal matchedQuantity,
                                     BigDecimal remainingQuantity,
                                     BigDecimal executionPriceArs,
                                     String message) {
        return new OrdenResponse(order.getId(), order.getStatus(), matchedQuantity, remainingQuantity,
                executionPriceArs, message);
    }

    /**
     * Valida que el usuario tenga las acciones para vender
     */
    private void validaDisponibilidadAcciones(OrdenEntity order) {
        log.debug("Validando disponibilidad de acciones para venta: userId={}, symbol={}, qty={}",
                order.getUserId(), order.getSymbol(), order.getQuantity());

        try {
            PortafolioClienteResponse portfolio = restTemplate.getForObject(
                    portafolioUrl + "/users/" + order.getUserId() + "/portfolio",
                    PortafolioClienteResponse.class);

            if (portfolio == null) {
                log.warn("⚠️ Portfolio no encontrado para userId: {}", order.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio no encontrado para el usuario");
            }

            // Buscar la posición del símbolo
            var position = portfolio.positions().stream()
                    .filter(p -> p.symbol().equalsIgnoreCase(order.getSymbol()))
                    .findFirst()
                    .orElse(null);

            if (position == null || position.quantity().compareTo(BigDecimal.ZERO) == 0) {
                log.warn("❌ Posición no disponible para venta: usuario={}, symbol={}",
                        order.getUserId(), order.getSymbol());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No tiene acciones de " + order.getSymbol() + " para vender");
            }

            if (position.quantity().compareTo(order.getQuantity()) < 0) {
                log.warn("❌ Cantidad insuficiente de acciones: usuario={}, symbol={}, " +
                        "requerida={}, disponible={}",
                        order.getUserId(), order.getSymbol(),
                        order.getQuantity(), position.quantity());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Cantidad insuficiente de %s. Tiene: %s, quiere vender: %s",
                                order.getSymbol(), position.quantity(), order.getQuantity()));
            }

            log.debug("✓ Disponibilidad validada: posee {} de {}",
                    position.quantity(), order.getSymbol());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando disponibilidad de acciones: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar disponibilidad de acciones");
        }
    }
}
