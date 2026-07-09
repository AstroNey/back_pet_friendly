package lns.back.backend_pet_friendly.domain.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Construction de {@link PageRequest} avec bornes de sécurité : la taille de page demandée par le
 * client est plafonnée pour éviter un DoS mémoire (ex. {@code ?size=100000000} chargerait toute une
 * table). Le numéro de page négatif est ramené à 0.
 */
final class Pagination {
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    private Pagination() {}

    static PageRequest of(int page, int size) {
        return PageRequest.of(safePage(page), safeSize(size));
    }

    static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(safePage(page), safeSize(size), sort);
    }

    private static int safePage(int page) {
        return Math.max(page, 0);
    }

    private static int safeSize(int size) {
        if (size < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
