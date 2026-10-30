package com.ucc.aiproxy.repository;

import com.ucc.aiproxy.model.Plan;
import com.ucc.aiproxy.model.UserAccount;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage of user accounts. New users are created on demand with the FREE plan.
 */
@Repository
public class UserAccountRepository {

    private final Map<String, UserAccount> accounts = new ConcurrentHashMap<>();
    private final Clock clock;

    public UserAccountRepository(Clock clock) {
        this.clock = clock;
    }

    public UserAccount getOrCreate(String userId) {
        return accounts.computeIfAbsent(userId, id -> new UserAccount(id, Plan.FREE, LocalDate.now(clock)));
    }

    public Collection<UserAccount> findAll() {
        return accounts.values();
    }

    public void clear() {
        accounts.clear();
    }
}
