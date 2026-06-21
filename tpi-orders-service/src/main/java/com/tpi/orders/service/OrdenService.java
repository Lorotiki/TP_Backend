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
        log.info(" Procesando orden de venta: usuarioID={}, simbolo={}, cantidad={}, precioLimite={}",
                request.userId(), request.simbolo(), request.cantidad(), request.precioLimite());
        try {
            var orden = createOrden(request, "SELL");
            validaDisponibilidadAcciones(orden);
            orden.setEstado("PENDING");
            ordenRepository.save(orden);

            log.info("✅ Orden de venta registrada: nroOrden={}, estado={}",
                    orden.getId(), orden.getEstado());

            recordHistorial("SELL_ORDER_CREATED",
                            orden, //orden
                            orden.getCantidad(), //cantidad
                            orden.getCantidadRestante(), //cantidadRemaninete
                            orden.getPrecioLimite(), //precio limite
                            "Orden de venta registrada");

            return toResponse(orden, BigDecimal.ZERO, orden.getCantidadRestante(),
                    orden.getPrecioLimite(), "Orden de venta registrada");
        } catch (Exception e) {
            log.error("Error procesando orden de venta: usuarioID={}, simbolo={}",
                    request.userId(), request.simbolo(), e);
            throw e;
        }
    }

    @Transactional
    public OrdenResponse comprar(OrdenRequest request) {
        log.info("▶ Procesando orden de compra: usuario={}, simbolo={}, cantidad={}, precioLimite={}",
                request.userId(), request.simbolo(), request.cantidad(), request.precioLimite());

        try {
            // Consultar cotización
            var cotizacionMercado = obtenerCotizacion(request.simbolo());
            log.debug("Cotización obtenida: simbolo={}, precio={}, moneda={}",
                    cotizacionMercado.simbolo(), cotizacionMercado.precio(), cotizacionMercado.moneda());

            var orden = createOrden(request, "BUY");
            log.debug(" Orden creada: nroOrden={}", orden.getId());

            // Validar balance
            validaSaldoUsuario(orden, cotizacionMercado.precio());
            log.debug(" Balance validado: saldoRequerido={} ARS",
                    orden.getCantidad().multiply(orden.getPrecioLimite()));

            ordenRepository.save(orden);
            log.debug(" Orden persistida en BD");

            // Matching
            var vendedoresDisponibles = ordenRepository.findCoincidenciasVentas(orden.getSimbolo(), "SELL");
            log.debug("Encontradas {} órdenes de venta disponibles", vendedoresDisponibles.size());

            var cantidades = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            for (var vendedor : vendedoresDisponibles) {
                if (orden.getCantidadRestante().compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("Cantidad completa satisfecha, saliendo del loop");
                    break;
                }

                if (vendedor.getUserId().equals(orden.getUserId())) {
                    log.debug("Saltando venta del mismo usuario: usuarioID={}", orden.getUserId());
                    continue;
                }

                if (vendedor.getPrecioLimite().compareTo(orden.getPrecioLimite()) > 0) {
                    log.debug("Precio no coincide: vendedor.precio={} > comprador.precio={}",
                            vendedor.getPrecioLimite(), orden.getPrecioLimite());
                    continue;
                }

                var cantidad = vendedor.getCantidadRestante().min(orden.getCantidadRestante())
                        .setScale(4, RoundingMode.HALF_UP);

                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("Cantidad inválida: cantidad={}", cantidad);
                    continue;
                }

                log.info("MATCH encontrado: comprador={}, vendedor={}, cantidad={}, precio={}",
                        orden.getUserId(), vendedor.getUserId(), cantidad, vendedor.getPrecioLimite());

                var precioVenta = vendedor.getPrecioLimite().setScale(4, RoundingMode.HALF_UP);

                ejecutarCompraXPortafolio(orden.getUserId(), "BUY", orden.getSimbolo(), cantidad,
                        precioVenta, orden.getId().toString());
                ejecutarCompraXPortafolio(vendedor.getUserId(), "SELL", vendedor.getSimbolo(), cantidad,
                        precioVenta, vendedor.getId().toString());

                //to-do deberia hacerse en el evento q ejecuta la compra y la venta anteriores

                vendedor.setCantidadRestante(vendedor.getCantidadRestante().subtract(cantidad));
                vendedor.setEstado(resolveStatus(vendedor.getCantidadRestante(), vendedor.getCantidad()));
                orden.setCantidadRestante(orden.getCantidadRestante().subtract(cantidad));
                orden.setEstado(resolveStatus(orden.getCantidadRestante(), orden.getCantidad()));

                cantidades = cantidades.add(cantidad);
            }

            ordenRepository.saveAll(List.of(orden));
            recordHistorial("BUY_ORDER_EXECUTED", orden, cantidades, orden.getCantidadRestante(),
                    orden.getPrecioLimite(), "Orden de compra ejecutada");

            log.info("Orden completada: ordenID={}, cantidadCoincidida={}, cantidadRestante={}, estado={}",
                    orden.getId(), cantidades, orden.getCantidadRestante(), orden.getEstado());

            return toResponse(orden, cantidades, orden.getCantidadRestante(),
                    orden.getPrecioLimite(), "Orden de compra ejecutada");
        } catch (Exception e) {
            log.error("Error procesando orden de compra: usuarioID={}, simbolo={}",
                    request.userId(), request.simbolo(), e);
            throw e;
        }
    }

    public List<OrdenResponse> getOrdenesByUsuario(String userId) {
        log.debug("Consultando órdenes para usuario: {}", userId);
        return ordenRepository.findByUsuarioIdOrdenByCreatedAtDesc(userId).stream()
                .map(orden -> toResponse(orden,
                        orden.getCantidad().subtract(orden.getCantidadRestante()),
                        orden.getCantidadRestante(),
                        orden.getPrecioLimite(),
                        "Consulta de órdenes"))
                .toList();
    }

    private void ejecutarCompraXPortafolio(String usuarioId, String lado, String simbolo, BigDecimal cantidad, BigDecimal precioArs, String referenciaId) {
        var request = new PortafolioTradeClienteRequest(lado, simbolo, cantidad, precioArs, referenciaId);
        try {
            restTemplate.postForEntity(portafolioUrl + "/users/" + usuarioId + "/portfolio/trades", request,
                    Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo actualizar portfolio-service", e);
        }
    }

    /**
     * Valida que el usuario tenga saldo suficiente para la compra
     */
    private void validaSaldoUsuario(OrdenEntity orden, BigDecimal precioMercado) {
        log.debug("Validando balance para compra: usuarioId={}, cantidad={}, precioLimite={}",
                orden.getUserId(), orden.getCantidad(), orden.getPrecioLimite());

        // Consultar portafolio del usuario
        try {
            PortafolioClienteResponse portafolio = restTemplate.getForObject(
                    portafolioUrl + "/users/" + orden.getUserId() + "/portfolio",
                    PortafolioClienteResponse.class);

            if (portafolio == null) {
                log.warn("Portafolio no encontrado para usuarioId: {}", orden.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portafolio no encontrado para el usuario");
            }

            // Calcular monto requerido
            BigDecimal montoRequerido = orden.getCantidad()
                    .multiply(orden.getPrecioLimite())
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Balance requerido: {} ARS, Balance disponible: {} ARS",
                    montoRequerido, portafolio.valorTotalArs());

            // Validar saldo
            if (portafolio.valorTotalArs().compareTo(montoRequerido) < 0) {
                log.warn("Saldo insuficiente: usuarioId={}, requerido={}, disponible={}",
                        orden.getUserId(), montoRequerido, portafolio.valorTotalArs());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Saldo insuficiente. Requerido: %s ARS, Disponible: %s ARS",
                                montoRequerido, portafolio.valorTotalArs()));
            }

            log.debug("Balance validado correctamente");

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

    private CotizacionClienteResponse obtenerCotizacion(String simbolo) {
        try {
            return restTemplate.getForObject(mercadoUrl + "/quotes/" + simbolo, CotizacionClienteResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar market-data-service", e);
        }
    }

    private OrdenEntity createOrden(OrdenRequest request, String lado) {
        var orden = new OrdenEntity();
        orden.setUserId(request.userId());
        orden.setSimbolo(request.simbolo().trim().toUpperCase(Locale.ROOT));
        orden.setLado(lado);
        orden.setCantidad(request.cantidad().setScale(4, RoundingMode.HALF_UP));
        orden.setCantidadRestante(request.cantidad().setScale(4, RoundingMode.HALF_UP));
        orden.setPrecioLimite(request.precioLimite().setScale(4, RoundingMode.HALF_UP));
        orden.setEstado("PENDING");
        return orden;
    }

    private String resolveStatus(BigDecimal cantidadRest, BigDecimal cantidadTotal) {
        if (cantidadRest.compareTo(BigDecimal.ZERO) == 0) {
            return "FILLED";
        }
        if (cantidadRest.compareTo(cantidadTotal) < 0) {
            return "PARTIALLY_FILLED";
        }
        return "PENDING";
    }

    private OrdenResponse toResponse(OrdenEntity orden, BigDecimal cantidadCoinc,
                                     BigDecimal cantidadRest,
                                     BigDecimal precioEjecArs,
                                     String mensaje) {
        return new OrdenResponse(orden.getId(), orden.getEstado(), cantidadCoinc, cantidadRest,
                precioEjecArs, mensaje);
    }

    /**
     * Valida que el usuario tenga las acciones para vender
     */
    private void validaDisponibilidadAcciones(OrdenEntity orden) {
        log.debug("Validando disponibilidad de acciones para venta: usuarioId={}, simbolo={}, cantidad={}",
                orden.getUserId(), orden.getSimbolo(), orden.getCantidad());

        try {
            PortafolioClienteResponse portafolio = restTemplate.getForObject(
                    portafolioUrl + "/users/" + orden.getUserId() + "/portfolio",
                    PortafolioClienteResponse.class);

            if (portafolio == null) {
                log.warn("Portafolio no encontrado para usuarioId: {}", orden.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portafolio no encontrado para el usuario");
            }

            // Buscar la posición del símbolo
            var posicion = portafolio.posiciones().stream()
                    .filter(p -> p.simbolo().equalsIgnoreCase(orden.getSimbolo()))
                    .findFirst()
                    .orElse(null);

            if (posicion == null || posicion.cantidad().compareTo(BigDecimal.ZERO) == 0) {
                log.warn("Posición no disponible para venta: usuarioId={}, simbolo={}",
                        orden.getUserId(), orden.getSimbolo());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No tiene acciones de " + orden.getSimbolo() + " para vender");
            }

            if (posicion.cantidad().compareTo(orden.getCantidad()) < 0) {
                log.warn("Cantidad insuficiente de acciones: usuarioId={}, simbolo={}, " +
                        "requerida={}, disponible={}",
                        orden.getUserId(), orden.getSimbolo(),
                        orden.getCantidad(), posicion.cantidad());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Cantidad insuficiente de %s. Tiene: %s, quiere vender: %s",
                                orden.getSimbolo(), posicion.cantidad(), orden.getCantidad()));
            }

            log.debug("Disponibilidad validada: posee {} de {}",
                    posicion.cantidad(), orden.getSimbolo());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando disponibilidad de acciones: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar disponibilidad de acciones");
        }
    }

    private void recordHistorial(String tipoEvento, OrdenEntity orden,
                                  BigDecimal cantidad, BigDecimal cantidadRestante,
                                  BigDecimal precioLimite, String mensaje) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("simbolo", orden.getSimbolo());
            payload.put("lado", orden.getLado());
            payload.put("cantidad", cantidad);
            payload.put("cantidadRestante", cantidadRestante);
            payload.put("precioLimite", precioLimite);
            payload.put("mensaje", mensaje);

            var request = new HistorialEventosClienteRequest(
                    new java.util.Random().nextInt(),
                    tipoEvento,
                    orden.getUserId(),
                    orden.getId(),
                    new java.util.Random().nextInt(),
                    null,
                    payload
            );

            restTemplate.postForObject(historialUrl + "/events", request, Void.class);
            log.debug("Evento registrado en historial: tipo={}, ordenId={}", tipoEvento, orden.getId());
        } catch (Exception e) {
            log.warn("Error registrando evento en historial: {}", e.getMessage());
        }
    }
}
