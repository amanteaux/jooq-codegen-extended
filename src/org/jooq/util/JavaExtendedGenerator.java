package org.jooq.util;

import java.util.List;

import org.jooq.DAO;
import org.jooq.impl.DAOImpl;
import org.jooq.tools.JooqLogger;
import org.jooq.util.GeneratorStrategy.Mode;

public class JavaExtendedGenerator extends JavaGenerator {

	private static final JooqLogger log = JooqLogger.getLogger(JavaExtendedGenerator.class);

	/**
	 * @return The abstract DAO all table DAO will extend
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends DAO<?, ?, ?>> getSuperDao() {
		return (Class<? extends DAO<?, ?, ?>>) DAOImpl.class;
	}

	@Override
	protected void generateDao(final TableDefinition table) {
		final String className = getStrategy().getJavaClassName(table, Mode.DAO);
		final String tableRecord = getStrategy().getFullJavaClassName(table, Mode.RECORD);
		final String daoImpl = getSuperDao().getName();

		String tType = "Void";
		String pType = getStrategy().getFullJavaClassName(table, Mode.POJO);

		UniqueKeyDefinition key = table.getPrimaryKey();
		ColumnDefinition keyColumn = null;

		if (key != null) {
			List<ColumnDefinition> columns = key.getKeyColumns();

			if (columns.size() == 1) {
				keyColumn = columns.get(0);
				tType = getJavaType(keyColumn.getType());
			}
		}

		// [#2573] Skip DAOs for tables that don't have 1-column-PKs (for now)
		if (keyColumn == null) {
			log.info("Skipping DAO generation", getStrategy().getFileName(table, Mode.DAO));
			return;
		} else {
			log.info("Generating DAO", getStrategy().getFileName(table, Mode.DAO));
		}

		JavaWriter out = new JavaWriter(getStrategy().getFile(table, Mode.DAO));
		printPackage(out, table, Mode.DAO);
		printClassJavadoc(out, table);

		out.println("public abstract class %s<P extends %s> extends %s<%s, P, %s> {", className, pType, daoImpl, tableRecord, tType);

		// Default constructor
		// -------------------
		out.tab(1).javadoc("{@inheritDoc}", className);
		out.tab(1).println("protected %s(org.jooq.Table<%s> table, Class<P> type, org.jooq.Configuration configuration) {", className, tableRecord);
		out.tab(2).println("super(table, type, configuration);");
		out.tab(1).println("}");

		// Template method implementations
		// -------------------------------
		out.tab(1).overrideInherit();
		out.tab(1).println("protected %s getId(P object) {", tType);
		out.tab(2).println("return object.%s();", getStrategy().getJavaGetterName(keyColumn, Mode.POJO));
		out.tab(1).println("}");

		for (ColumnDefinition column : table.getColumns()) {
			final String colName = column.getOutputName();
			final String colClass = getStrategy().getJavaClassName(column, Mode.POJO);
			final String colType = getJavaType(column.getType());
			final String colIdentifier = getStrategy().getFullJavaIdentifier(column);

			// fetchBy[Column]([T]...)
			// -----------------------
			out.tab(1).javadoc("Fetch records that have <code>%s IN (values)</code>", colName);
			out.tab(1).println("public %s<P> fetchBy%s(%s... values) {", List.class, colClass, colType);
			out.tab(2).println("return fetch(%s, values);", colIdentifier);
			out.tab(1).println("}");

			// fetchOneBy[Column]([T])
			// -----------------------
			ukLoop: for (UniqueKeyDefinition uk : column.getUniqueKeys()) {

				// If column is part of a single-column unique key...
				if (uk.getKeyColumns().size() == 1 && uk.getKeyColumns().get(0).equals(column)) {
					out.tab(1).javadoc("Fetch a unique record that has <code>%s = value</code>", colName);
					out.tab(1).println("public P fetchOneBy%s(%s value) {", colClass, colType);
					out.tab(2).println("return fetchOne(%s, value);", colIdentifier);
					out.tab(1).println("}");

					break ukLoop;
				}
			}
		}

		out.println("}");
		out.close();
	}

}
