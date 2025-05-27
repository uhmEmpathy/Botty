package util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class RoleValidator {
    private static final String ROLE_FILE = "data/privileges/roles.json";

    public enum Role {
        STAFF,
        ADMIN
    }

    static {
        ensureRoleFileExists();
    }

    public static boolean isAdmin(String userId) {
        return getRole(userId) == Role.ADMIN;
    }

    public static boolean isStaff(String userId) {
        Role role = getRole(userId);
        return role == Role.STAFF || role == Role.ADMIN;
    }

    public static boolean isStaffOrAdmin(String userId) {
        return isStaff(userId);
    }

    private static Role getRole(String userId) {
        try {
            File file = new File(ROLE_FILE);
            if (!file.exists()) return null;

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> roleMap = mapper.readValue(file, Map.class);
            String value = roleMap.get(userId);

            if (value == null) return null;
            return Role.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static void ensureRoleFileExists() {
        try {
            File file = new File(ROLE_FILE);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                Map<String, String> defaultData = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                try (FileWriter writer = new FileWriter(file)) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(writer, defaultData);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize RoleValidator file: " + e.getMessage());
        }
    }

    public static void setRole(String userId, Role role) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(ROLE_FILE);
            Map<String, String> roleMap = file.exists()
                    ? mapper.readValue(file, Map.class)
                    : new HashMap<>();

            roleMap.put(userId, role.name());

            try (FileWriter writer = new FileWriter(file)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, roleMap);
            }
        } catch (Exception e) {
            System.err.println("Failed to set role: " + e.getMessage());
        }
    }

}