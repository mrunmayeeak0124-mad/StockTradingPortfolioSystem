package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The favourite assets of one trader. A Set means an asset can appear only once.
 */
public class Watchlist {

    private final Set<String> assetIds = new LinkedHashSet<>();

    /** Returns true if the asset was added (false if it was already there). */
    public boolean add(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new InvalidInputException("Asset ID cannot be empty.");
        }
        return assetIds.add(assetId);
    }

    public boolean remove(String assetId) {
        return assetIds.remove(assetId);
    }

    public boolean contains(String assetId) {
        return assetIds.contains(assetId);
    }

    public boolean contains(Asset asset) {
        return asset != null && contains(asset.getAssetId());
    }

    public int size() {
        return assetIds.size();
    }

    /** A copy, so outside code cannot change the set. */
    public Set<String> getAssetIds() {
        return new LinkedHashSet<>(assetIds);
    }
}