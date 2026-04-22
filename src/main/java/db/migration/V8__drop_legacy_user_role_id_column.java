package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class V8__drop_legacy_user_role_id_column extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (!columnExists(connection, "users", "role_id")) {
            return;
        }

        for (String constraintName : findForeignKeysForColumn(connection, "users", "role_id")) {
            execute(connection, "ALTER TABLE users DROP CONSTRAINT " + quoteIdentifier(connection, constraintName));
        }

        execute(connection, "ALTER TABLE users DROP COLUMN role_id");
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        for (String tableVariant : variants(tableName)) {
            for (String columnVariant : variants(columnName)) {
                try (ResultSet columns = metaData.getColumns(null, null, tableVariant, columnVariant)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Set<String> findForeignKeysForColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> constraintNames = new LinkedHashSet<>();

        for (String tableVariant : variants(tableName)) {
            try (ResultSet importedKeys = metaData.getImportedKeys(null, null, tableVariant)) {
                while (importedKeys.next()) {
                    String fkColumnName = importedKeys.getString("FKCOLUMN_NAME");
                    String fkName = importedKeys.getString("FK_NAME");

                    if (fkName != null && columnName.equalsIgnoreCase(fkColumnName)) {
                        constraintNames.add(fkName);
                    }
                }
            }
        }

        return constraintNames;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String quoteIdentifier(Connection connection, String identifier) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString();

        if (quote == null || quote.isBlank()) {
            return identifier;
        }

        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private Set<String> variants(String identifier) {
        Set<String> values = new LinkedHashSet<>();
        values.add(identifier);
        values.add(identifier.toLowerCase(Locale.ROOT));
        values.add(identifier.toUpperCase(Locale.ROOT));
        return values;
    }
}
