package com.tpi.portfolio.service;

import com.tpi.portfolio.dto.DepositRequest;
import com.tpi.portfolio.dto.DepositResponse;
import com.tpi.portfolio.dto.PortfolioResponse;
import com.tpi.portfolio.dto.PositionResponse;
import com.tpi.portfolio.dto.TradeAdjustmentRequest;
import com.tpi.portfolio.entity.Account;
import com.tpi.portfolio.entity.CashMovement;
import com.tpi.portfolio.entity.Position;
import com.tpi.portfolio.repository.AccountRepository;
import com.tpi.portfolio.repository.CashMovementRepository;
import com.tpi.portfolio.repository.PositionRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class PortfolioService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final CashMovementRepository cashMovementRepository;

    public PortfolioService(AccountRepository accountRepository,
                            PositionRepository positionRepository,
                            CashMovementRepository cashMovementRepository) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.cashMovementRepository = cashMovementRepository;
    }

    @Transactional
    public PortfolioResponse getPortfolio(String userId) {
        var account = getOrCreateAccount(userId);
        var positions = positionRepository.findByAccountId(account.getId()).stream()
                .map(position -> new PositionResponse(position.getSymbol(), position.getQuantity(), position.getAvgPriceArs()))
                .toList();
        return new PortfolioResponse(account.getUserId(), account.getBalanceArs(), positions);
    }

    @Transactional
    public DepositResponse deposit(String userId, DepositRequest request) {
        var account = getOrCreateAccount(userId);
        account.setBalanceArs(account.getBalanceArs().add(scaleMoney(request.amountArs())));
        accountRepository.save(account);

        var movement = new CashMovement();
        movement.setAccountId(account.getId());
        movement.setType("DEPOSIT");
        movement.setAmountArs(scaleMoney(request.amountArs()));
        movement.setReferenceId(request.referenceId());
        cashMovementRepository.save(movement);

        return new DepositResponse(movement.getId(), account.getBalanceArs());
    }

    @Transactional
    public PortfolioResponse applyTrade(String userId, TradeAdjustmentRequest request) {
        var account = getOrCreateAccount(userId);
        var side = normalize(request.side());
        var symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        var quantity = request.quantity().setScale(4, RoundingMode.HALF_UP);
        var price = scalePrice(request.priceArs());
        var totalAmount = scaleMoney(price.multiply(quantity));

        if ("BUY".equals(side)) {
            if (account.getBalanceArs().compareTo(totalAmount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para concretar la compra");
            }
            account.setBalanceArs(account.getBalanceArs().subtract(totalAmount));
            var position = positionRepository.findByAccountIdAndSymbol(account.getId(), symbol)
                    .orElseGet(() -> createPosition(account.getId(), symbol));
            var currentQty = defaultQuantity(position.getQuantity());
            var currentAmount = currentQty.multiply(position.getAvgPriceArs());
            var newQty = currentQty.add(quantity);
            var newAmount = currentAmount.add(price.multiply(quantity));
            position.setQuantity(newQty);
            position.setAvgPriceArs(newAmount.divide(newQty, 4, RoundingMode.HALF_UP));
            positionRepository.save(position);
            saveMovement(account.getId(), "BUY_DEBIT", totalAmount, request.referenceId());
        } else if ("SELL".equals(side)) {
            var position = positionRepository.findByAccountIdAndSymbol(account.getId(), symbol)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no posee tenencias para el símbolo " + symbol));
            if (position.getQuantity().compareTo(quantity) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad insuficiente para vender");
            }
            position.setQuantity(position.getQuantity().subtract(quantity));
            if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                positionRepository.delete(position);
            } else {
                positionRepository.save(position);
            }
            account.setBalanceArs(account.getBalanceArs().add(totalAmount));
            saveMovement(account.getId(), "SELL_CREDIT", totalAmount, request.referenceId());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lado de operación inválido");
        }

        accountRepository.save(account);
        return getPortfolio(userId);
    }

    private Position createPosition(java.util.UUID accountId, String symbol) {
        var position = new Position();
        position.setAccountId(accountId);
        position.setSymbol(symbol);
        position.setQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        position.setAvgPriceArs(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        return position;
    }

    private BigDecimal defaultQuantity(BigDecimal quantity) {
        return quantity == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : quantity;
    }

    private void saveMovement(java.util.UUID accountId, String type, BigDecimal amount, String referenceId) {
        var movement = new CashMovement();
        movement.setAccountId(accountId);
        movement.setType(type);
        movement.setAmountArs(scaleMoney(amount));
        movement.setReferenceId(referenceId);
        cashMovementRepository.save(movement);
    }

    private Account getOrCreateAccount(String userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> accountRepository.save(createAccount(userId)));
    }

    private Account createAccount(String userId) {
        var account = new Account();
        account.setUserId(userId);
        account.setBalanceArs(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return account;
    }

    private String normalize(String side) {
        return side == null ? "" : side.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}

