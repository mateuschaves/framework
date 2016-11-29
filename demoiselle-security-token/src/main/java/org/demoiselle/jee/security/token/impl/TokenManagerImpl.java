/*
 * Demoiselle Framework
 *
 * License: GNU Lesser General Public License (LGPL), version 3 or later.
 * See the lgpl.txt file in the root directory or <https://www.gnu.org/licenses/lgpl.html>.
 */
package org.demoiselle.jee.security.token.impl;

import static java.util.UUID.randomUUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import org.demoiselle.jee.core.api.security.DemoisellePrincipal;
import org.demoiselle.jee.core.api.security.Token;
import org.demoiselle.jee.core.api.security.TokenManager;

/**
 *
 * @author 70744416353
 */
@ApplicationScoped
@Priority(AUTHENTICATION)
public class TokenManagerImpl implements TokenManager {
    
    private final ConcurrentHashMap<String, DemoisellePrincipal> repo = new ConcurrentHashMap<>();
    
    @Inject
    private Token token;

    /**
     * Returns the user that is stored in a list in memory, from the token sent
     * in http header
     *
     * @return org.demoiselle.jee.core.api.security.DemoisellePrincipal
     */
    @Override
    public DemoisellePrincipal getUser() {
        if (token.getKey() != null && !token.getKey().isEmpty()) {
            return repo.get(token.getKey());
        }
        return null;
    }

    /**
     * It will be included in the user memory and generate a unique
     * identification token to be placed in the header of HTTP requests
     *
     * @param user org.demoiselle.jee.core.api.security.DemoisellePrincipal
     */
    @Override
    public void setUser(DemoisellePrincipal user) {
        token.setKey(null);
        
        repo.entrySet().stream().parallel().filter((entry) -> (entry.getValue().getIdentity().equalsIgnoreCase(user.getIdentity()))).forEach((entry) -> {
            token.setKey(entry.getKey());
        });
        
        if (token.getKey() == null) {
            String value = randomUUID().toString();
            repo.putIfAbsent(value, user.clone());
            token.setKey(value);
        }
        
        token.setType("Token");
    }

    /**
     * validate the token and the user is in memory
     *
     * @return boolean
     */
    @Override
    public boolean validate() {
        return getUser() != null;
    }

    /**
     * remove the token and the user is in memory
     */
    public void removeToken() {
        repo.remove(token.getKey());
        token.setKey(null);
    }

    /**
     * remove the token and the user is in memory
     *
     * @param user
     */
    @Override
    public void removeUser(DemoisellePrincipal user) {
        repo.entrySet().stream().parallel().filter((entry) -> (entry.getValue().getIdentity().equalsIgnoreCase(user.getIdentity()))).forEach((entry) -> {
            token.setKey(entry.getKey());
        });
        removeToken();
    }
    
}
