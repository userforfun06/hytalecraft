package com.proxy.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * HytaleCraft Block Registry — maps Hytale block IDs ↔ Minecraft block state IDs.
 *
 * Loading order:
 *  1. Built-in defaults from src/main/resources/block_mappings.json (bundled in JAR)
 *  2. External override at plugins/hytalecraft/block_mappings.json (if present)
 *
 * Fallback for unknown IDs: 0 (air in both systems)
 */
public class BlockRegistry {

    private static final Logger log = LoggerFactory.getLogger(BlockRegistry.class);

    /** Hytale block ID → MC block state ID */
    private final Map<Integer, Integer> hytaleToMC = new HashMap<>();
    /** MC block state ID → Hytale block ID */
    private final Map<Integer, Integer> mcToHytale = new HashMap<>();

    /** Singleton */
    private static BlockRegistry INSTANCE;

    public static BlockRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BlockRegistry();
            INSTANCE.loadDefaults();
            INSTANCE.tryLoadExternal();
        }
        return INSTANCE;
    }

    /** Package-private for testing */
    BlockRegistry() {}

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /**
     * Translate a Hytale block ID to the corresponding MC block state ID.
     * Returns 0 (air) if the mapping is unknown.
     */
    public int toMC(int hytaleId) {
        return hytaleToMC.getOrDefault(hytaleId, 0);
    }

    /**
     * Translate an MC block state ID to the corresponding Hytale block ID.
     * Returns 0 (air) if the mapping is unknown.
     */
    public int toHytale(int mcStateId) {
        return mcToHytale.getOrDefault(mcStateId, 0);
    }

    public int size() {
        return hytaleToMC.size();
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    private void loadDefaults() {
        try (InputStream is = getClass().getResourceAsStream("/block_mappings.json")) {
            if (is == null) {
                log.warn("[BlockRegistry] Built-in block_mappings.json not found in JAR resources.");
                seedHardcodedDefaults();
                return;
            }
            parseMappingsJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), "built-in");
        } catch (Exception e) {
            log.error("[BlockRegistry] Failed to load built-in mappings; falling back to hardcoded defaults", e);
            seedHardcodedDefaults();
        }
    }

    private void tryLoadExternal() {
        Path external = Paths.get("plugins/hytalecraft/block_mappings.json");
        if (!Files.exists(external)) return;
        try {
            String json = Files.readString(external, StandardCharsets.UTF_8);
            parseMappingsJson(json, external.toString());
            log.info("[BlockRegistry] Loaded external mappings from {}", external);
        } catch (Exception e) {
            log.warn("[BlockRegistry] Failed to load external mappings from {}: {}", external, e.getMessage());
        }
    }

    /**
     * Minimal hand-rolled JSON parser — avoids adding a JSON library dependency.
     * Expects the structure: { "mappings": [ { "hytaleId": N, "mcStateId": M }, ... ] }
     */
    private void parseMappingsJson(String json, String source) {
        // Extract the mappings array content
        int start = json.indexOf("\"mappings\"");
        if (start < 0) {
            log.warn("[BlockRegistry] No 'mappings' key found in {}", source);
            return;
        }
        int arrStart = json.indexOf('[', start);
        int arrEnd   = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0) {
            log.warn("[BlockRegistry] Malformed mappings array in {}", source);
            return;
        }
        String arr = json.substring(arrStart + 1, arrEnd);

        // Split into individual objects by '}' boundaries
        int loaded = 0;
        int cursor = 0;
        while (cursor < arr.length()) {
            int objStart = arr.indexOf('{', cursor);
            int objEnd   = arr.indexOf('}', objStart + 1);
            if (objStart < 0 || objEnd < 0) break;

            String obj = arr.substring(objStart + 1, objEnd);
            Integer hytaleId  = extractInt(obj, "hytaleId");
            Integer mcStateId = extractInt(obj, "mcStateId");

            if (hytaleId != null && mcStateId != null) {
                register(hytaleId, mcStateId);
                loaded++;
            }
            cursor = objEnd + 1;
        }
        log.info("[BlockRegistry] Loaded {} block mappings from {}", loaded, source);
    }

    private Integer extractInt(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return null;
        int colon = obj.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        // Read digits (possibly negative)
        int i = colon + 1;
        while (i < obj.length() && (obj.charAt(i) == ' ' || obj.charAt(i) == '\t')) i++;
        int numStart = i;
        if (i < obj.length() && obj.charAt(i) == '-') i++;
        while (i < obj.length() && Character.isDigit(obj.charAt(i))) i++;
        if (i == numStart) return null;
        try {
            return Integer.parseInt(obj.substring(numStart, i).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void register(int hytaleId, int mcStateId) {
        hytaleToMC.put(hytaleId, mcStateId);
        mcToHytale.put(mcStateId, hytaleId);
    }

    /** Hard-coded fallback in case the resource file is missing. */
    private void seedHardcodedDefaults() {
        register(1,  0);   // grass_block
        register(2,  10);  // dirt
        register(3,  1);   // stone
        register(4,  66);  // sand
        register(5,  68);  // gravel
        register(6,  84);  // oak_log
        register(7,  161); // oak_leaves
        register(8,  34);  // water
        register(9,  33);  // bedrock
        register(10, 17);  // oak_planks
        log.info("[BlockRegistry] Seeded {} hardcoded default mappings", hytaleToMC.size());
    }
}
