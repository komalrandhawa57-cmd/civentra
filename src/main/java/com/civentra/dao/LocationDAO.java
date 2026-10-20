package com.civentra.dao;

import com.civentra.model.Location;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class LocationDAO {

    private final DataSource dataSource;

    public LocationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Location> getActiveLocations() throws SQLException {
        List<Location> locations = getAllLocations();
        return locations.stream().filter(Location::isActive).toList();
    }

    public List<Location> getAllLocations() throws SQLException {
        List<Location> list = new ArrayList<>();
        String sql = "SELECT * FROM locations ORDER BY location_id ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToLocation(rs));
            }
        }

        return list;
    }

    public Location getLocationById(int locationId) throws SQLException {
        String sql = "SELECT * FROM locations WHERE location_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLocation(rs);
                }
            }
        }
        return null;
    }

    public boolean setLocationActive(int locationId, boolean active) throws SQLException {
        String sql = "UPDATE locations SET is_active = ? WHERE location_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, active ? 1 : 0);
            stmt.setInt(2, locationId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Location mapResultSetToLocation(ResultSet rs) throws SQLException {
        Location loc = new Location();
        loc.setLocationId(rs.getInt("location_id"));
        Set<String> columns = getColumnNames(rs.getMetaData());

        loc.setWardName(firstNonBlank(rs, columns, "ward_name", "name"));
        loc.setWardNumber(firstNonBlank(rs, columns, "ward_number"));
        loc.setAreaName(firstNonBlank(rs, columns, "area_name", "address"));
        loc.setCity(firstNonBlank(rs, columns, "city"));
        loc.setPincode(firstNonBlank(rs, columns, "pincode"));

        loc.setLatitude(rs.getDouble("latitude"));
        loc.setLongitude(rs.getDouble("longitude"));

        loc.setActive(rs.getInt("is_active") == 1);
        return loc;
    }

    private Set<String> getColumnNames(ResultSetMetaData metadata) throws SQLException {
        Set<String> columns = new HashSet<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            columns.add(metadata.getColumnName(i).toLowerCase(Locale.ROOT));
            columns.add(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        return columns;
    }

    private String firstNonBlank(ResultSet rs, Set<String> columns, String... candidates) throws SQLException {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase(Locale.ROOT))) {
                String value = rs.getString(candidate);
                if (value != null && !value.isBlank()) return value;
            }
        }
        return null;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
