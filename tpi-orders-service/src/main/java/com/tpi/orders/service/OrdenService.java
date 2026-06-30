package com.tpi.orders.service;

import com.tpi.orders.dto.*;
import com.tpi.orders.entity.OrdenEntity;
import com.tpi.orders.entity.OrdenFill;
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
import java.util.ArrayList;

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
    public OrdenResponse createOrden(OrdenRequest request) {
        log.info(" Procesando orden: lado={}, usuario={}, simbolo={}, cantidad={}, precioLimite={}", request.side(), request.userId(), request.symbol(), request.quantity(), request.limitPrice());

        if ("BUY".equalsIgnoreCase(request.side())) {
            return procesarCompra(request);
        } else if ("SELL".equalsIgnoreCase(request.side())) {
            return procesarVenta(request);
        } else {
            throw new IllegalArgumentException("El campo 'side' debe ser 'BUY' o 'SELL'.");
        }
    }

    private OrdenResponse procesarVenta(OrdenRequest request) {
        try {
            
            OrdenEntity orden = createOrdenEntity(request);

            validaDisponibilidadAcciones(orden);

            orden.setEstado("PENDING");

            ordenRepository.save(orden);

            log.info("Orden de venta registrada: nroOrden={}, estado={}", orden.getId(), orden.getEstado());

            recordHistorial("SELL_ORDER_CREATED", orden, orden.getCantidad(), orden.getCantidadRestante(), orden.getPrecioLimite(), "Orden de venta registrada");

            return toResponse(orden, BigDecimal.ZERO, orden.getCantidadRestante(), orden.getPrecioLimite(), "Orden de venta registrada");
        
        } catch (Exception e) {
            log.error("Error procesando orden de venta: usuarioID={}, simbolo={}",
                    request.userId(), request.symbol(), e);
            throw e;
        }
    }

    private OrdenResponse procesarCompra(OrdenRequest request) {
        try {
            CotizacionClienteResponse cotizacionMercado = obtenerCotizacion(request.symbol());
            log.debug("Cotización obtenida: simbolo={}, precio={}, moneda={}", cotizacionMercado.simbolo(), cotizacionMercado.precio(), cotizacionMercado.moneda());

            OrdenEntity orden = createOrdenEntity(request);
            log.debug(" Orden creada: nroOrden={}", orden.getId());

            validaSaldoUsuario(orden, cotizacionMercado.precio());

            log.debug(" Balance validado: saldoRequerido={} ARS", orden.getCantidad().multiply(orden.getPrecioLimite()));

            ordenRepository.save(orden);
            
            log.debug(" Orden persistida en BD");

            List<OrdenEntity> vendedoresDisponibles = ordenRepository.findCoincidenciasVentas(orden.getSimbolo(), "SELL");
            
            log.debug("Encontradas {} órdenes de venta disponibles", vendedoresDisponibles.size());

            BigDecimal cantidades = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            
            List<OrdenEntity> vendedoresActualizados = new ArrayList<>();

            for (OrdenEntity vendedor : vendedoresDisponibles) {
                if (orden.getCantidadRestante().compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("La orden de compra ya fue completada");
                    break;
                }

                if (vendedor.getUserId().equals(orden.getUserId())) {
                    log.debug("Saltando venta del mismo usuario: usuarioID={}", orden.getUserId());
                    continue;
                }

                if (vendedor.getPrecioLimite().compareTo(orden.getPrecioLimite()) > 0) {
                    log.debug("El precio del vendedor no es competitivo: vendedor.precio={} > comprador.precio={}", vendedor.getPrecioLimite(), orden.getPrecioLimite());
                    continue;
                }

                // aca tengo que ver cual de las dos cantidades es menor, la del comprador o la del vendedor
                BigDecimal cantidad = vendedor.getCantidadRestante().min(orden.getCantidadRestante()).setScale(4, RoundingMode.HALF_UP);

                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("Cantidad inválida: cantidad={}", cantidad);
                    continue;
                }

                log.info("MATCH encontrado: comprador={}, vendedor={}, cantidad={}, precio={}", orden.getUserId(), vendedor.getUserId(), cantidad, vendedor.getPrecioLimite());

                BigDecimal precioVenta = vendedor.getPrecioLimite().setScale(4, RoundingMode.HALF_UP);

                ejecutarCompraXPortafolio(orden.getUserId(), "BUY", orden.getSimbolo(), cantidad, precioVenta, orden.getId().toString());
                ejecutarCompraXPortafolio(vendedor.getUserId(), "SELL", vendedor.getSimbolo(), cantidad, precioVenta, vendedor.getId().toString());

                vendedor.setCantidadRestante(vendedor.getCantidadRestante().subtract(cantidad));
                vendedor.setEstado(resolveStatus(vendedor.getCantidadRestante(), vendedor.getCantidad()));

                orden.setCantidadRestante(orden.getCantidadRestante().subtract(cantidad));
                orden.setEstado(resolveStatus(orden.getCantidadRestante(), orden.getCantidad()));
                
                vendedoresActualizados.add(vendedor);

                OrdenFill fill = new OrdenFill();
                fill.setCompraOrdenId(orden.getId());
                fill.setVentaOrdenId(vendedor.getId());
                fill.setSimbolo(orden.getSimbolo());
                fill.setCantidad(cantidad.setScale(4, RoundingMode.HALF_UP));
                fill.setPrecioArs(precioVenta);
                ordenFillRepository.save(fill);

                recordHistorial("SELL_ORDER_EXECUTED", vendedor, cantidad, vendedor.getCantidadRestante(), precioVenta, "Orden de venta ejecutada");

                cantidades = cantidades.add(cantidad);
            }

            // si la cantidad es cero quiere decir que no se pudo hacer la transaccion digamos
            if (cantidades.compareTo(BigDecimal.ZERO) == 0) {

                orden.setEstado("REJECTED");

                ordenRepository.save(orden);

                recordHistorial("BUY_ORDER_REJECTED", orden, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), orden.getCantidadRestante(), orden.getPrecioLimite(), "Orden de compra rechazada: no hay ofertas compatibles");
                
                return toResponse(orden, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), 
                        orden.getCantidadRestante(),
                        orden.getPrecioLimite(), "Orden de compra rechazada: no hay ofertas compatibles");
            }

            // si la cantidad no es cero osea que es mayor, algo se hizo 
            vendedoresActualizados.add(orden);

            ordenRepository.saveAll(vendedoresActualizados);

            recordHistorial("BUY_ORDER_EXECUTED", orden, cantidades, orden.getCantidadRestante(),
                    orden.getPrecioLimite(), "Orden de compra ejecutada");

            log.info("Orden completada: ordenID={}, cantidadCoincidida={}, cantidadRestante={}, estado={}", orden.getId(), cantidades, orden.getCantidadRestante(), orden.getEstado());

            return toResponse(orden, cantidades, orden.getCantidadRestante(),
                    orden.getPrecioLimite(), "Orden de compra ejecutada");

        } catch (Exception e) {
            log.error("Error procesando orden de compra: usuarioID={}, simbolo={}", request.userId(), request.symbol(), e);
            throw e;
        }
    }

    // esta es de usuario
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
            restTemplate.postForEntity(portafolioUrl + "/users/" + usuarioId + "/trades", request, Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo actualizar portfolio-service", e);
        }
    }

    private void validaSaldoUsuario(OrdenEntity orden, BigDecimal precioMercado) {
        log.debug("Validando balance para compra: usuarioId={}, cantidad={}, precioLimite={}",
                orden.getUserId(), orden.getCantidad(), orden.getPrecioLimite());

        // Consultar portafolio del usuario
        try {
            PortafolioClienteResponse portafolio = restTemplate.getForObject( portafolioUrl + "/users/" + orden.getUserId() + "/portfolio", PortafolioClienteResponse.class);

            if (portafolio == null) {
                log.warn("Portafolio no encontrado para usuarioId: {}", orden.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portafolio no encontrado para el usuario");
            }

            if (portafolio.valorTotalArs() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Respuesta invalida de portfolio-service: falta balanceArs");
            }

            BigDecimal montoRequerido = orden.getCantidad()
                    .multiply(orden.getPrecioLimite())
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Balance requerido: {} ARS, Balance disponible: {} ARS", montoRequerido, portafolio.valorTotalArs());

            if (portafolio.valorTotalArs().compareTo(montoRequerido) < 0) {
                log.warn("Saldo insuficiente: usuarioId={}, requerido={}, disponible={}", orden.getUserId(), montoRequerido, portafolio.valorTotalArs());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Saldo insuficiente. Requerido: %s ARS, Disponible: %s ARS",
                                montoRequerido, portafolio.valorTotalArs()));
            }

            log.debug("Balance validado correctamente");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando balance: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar saldo del usuario");
        }
    }

    private CotizacionClienteResponse obtenerCotizacion(String simbolo) {
        try {
            return restTemplate.getForObject(mercadoUrl + "/quotes/" + simbolo, CotizacionClienteResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar market-data-service", e);
        }
    }

    private OrdenEntity createOrdenEntity(OrdenRequest request) {
        OrdenEntity orden = new OrdenEntity();
        orden.setUserId(request.userId());
        orden.setSimbolo(request.symbol().trim().toUpperCase(Locale.ROOT));
        orden.setLado(request.side());
        orden.setCantidad(request.quantity().setScale(4, RoundingMode.HALF_UP));
        orden.setCantidadRestante(request.quantity().setScale(4, RoundingMode.HALF_UP));
        orden.setPrecioLimite(request.limitPrice().setScale(4, RoundingMode.HALF_UP));
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

    private void validaDisponibilidadAcciones(OrdenEntity orden) {
        log.debug("Validando disponibilidad de acciones para venta: usuarioId={}, simbolo={}, cantidad={}", orden.getUserId(), orden.getSimbolo(), orden.getCantidad());

        try {

            PortafolioClienteResponse portafolio = restTemplate.getForObject(portafolioUrl + "/users/" + orden.getUserId() + "/portfolio", PortafolioClienteResponse.class);

            if (portafolio == null) {
                log.warn("Portafolio no encontrado para usuarioId: {}", orden.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portafolio no encontrado para el usuario");
            }

            if (portafolio.posiciones() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Respuesta invalida de portfolio-service: falta positions");
            }

            
            var posicion = portafolio.posiciones().stream()
                    .filter(p -> p.simbolo().equalsIgnoreCase(orden.getSimbolo()))
                    .findFirst()
                    .orElse(null);

            if (posicion == null || posicion.cantidad().compareTo(BigDecimal.ZERO) == 0) {
                log.warn("Posición no disponible para venta: usuarioId={}, simbolo={}", orden.getUserId(), orden.getSimbolo());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No tiene acciones de " + orden.getSimbolo() + " para vender");
            }

            if (posicion.cantidad().compareTo(orden.getCantidad()) < 0) {
                log.warn("Cantidad insuficiente de acciones: usuarioId={}, simbolo={}, " + "requerida={}, disponible={}", orden.getUserId(), orden.getSimbolo(), orden.getCantidad(), posicion.cantidad());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Cantidad insuficiente de %s. Tiene: %s, quiere vender: %s",
                                orden.getSimbolo(), posicion.cantidad(), orden.getCantidad()));
            }

            log.debug("Disponibilidad validada: posee {} de {}", posicion.cantidad(), orden.getSimbolo());

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

            HistorialEventosClienteRequest request = new HistorialEventosClienteRequest(
                    java.util.UUID.randomUUID(),
                    tipoEvento,
                    orden.getUserId(),
                    orden.getId(),
                    orden.getId(),
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
