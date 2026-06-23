package lns.back.backend_pet_friendly.domain.model;

/** Rôle d'un utilisateur — base du RBAC (Spring Security : authority {@code ROLE_<name>}). */
public enum Role {
    USER,
    ADMIN
}