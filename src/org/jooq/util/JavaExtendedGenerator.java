package org.jooq.util;

import java.io.File;
import java.util.List;

import org.jooq.tools.JooqLogger;
import org.jooq.tools.StopWatch;
import org.jooq.util.ExtendedGeneratorStrategy.ModeExtended;
import org.jooq.util.GeneratorStrategy.Mode;

public class JavaExtendedGenerator extends JavaGenerator {

	private static final JooqLogger log = JooqLogger.getLogger(JavaExtendedGenerator.class);

	private final StopWatch watch = new StopWatch();
	private final ExtendedGeneratorStrategy extendedGeneratorStrategy;

	/**
	 * Force the use of an ExtendedGeneratorStrategy
	 * 
	 * @param extendedGeneratorStrategy
	 */
	public JavaExtendedGenerator(final ExtendedGeneratorStrategy extendedGeneratorStrategy) {
		super.setStrategy(extendedGeneratorStrategy);
		this.extendedGeneratorStrategy = extendedGeneratorStrategy;
	}

	public ExtendedGeneratorStrategy getExtendedStrategy() {
		return extendedGeneratorStrategy;
	}

	@Override
	public void setStrategy(final GeneratorStrategy strategy) {
		throw new RuntimeException("The strategy has already been set at the object initialisation : " + getExtendedStrategy().getClass());
	}

	// Entities generated once

	public final void generateWithChildEntities(final Database db) {
		generate(db);

		log.info("Generating child entities once");
		for (SchemaDefinition schema : db.getSchemata()) {
			try {
				if (db.getTables(schema).size() > 0) {
					generateChildEntities(db, schema);
				}
			} catch (Exception e) {
				throw new GeneratorException("Error generating code for schema " + schema, e);
			}
		}
		watch.splitInfo("EXTENDED GENERATION FINISHED!");
	}

	protected void generateChildEntities(final Database db, final SchemaDefinition schema) {
		generateBeansOnce(db, schema);
		generateDaosOnce(db, schema);
	}

	protected void generateBeansOnce(final Database db, final SchemaDefinition schema) {
		log.info("Generating child Beans once");

		for (TableDefinition table : db.getTables(schema)) {
			try {
				generateBeanOnce(table);
			} catch (Exception e) {
				log.error("Error while generating table DAO " + table, e);
			}
		}

		watch.splitInfo("Child beans generated");
	}

	protected void generateBeanOnce(final TableDefinition table) {
		final String className = getExtendedStrategy().getJavaClassName(table, ModeExtended.BEAN);

		final File beanFile = getExtendedStrategy().getFile(table, ModeExtended.BEAN);
		if (beanFile.exists()) {
			log.info("The child Bean is already generated", className);
		} else {
			log.info("Generating child Bean", getExtendedStrategy().getFileName(table, ModeExtended.BEAN));

			final String parentPojo = getExtendedStrategy().getFullJavaClassName(table, Mode.POJO);

			JavaWriter out = new JavaWriter(beanFile);
			printPackage(out, table, ModeExtended.BEAN);
			printExtendedClassJavadoc(out, table);

			out.println("public class %s extends %s {", className, parentPojo);

			out.printSerial();

			out.println("}");
			out.close();
		}
	}

	protected void generateDaosOnce(final Database db, final SchemaDefinition schema) {
		log.info("Generating child DAOs once");

		for (TableDefinition table : db.getTables(schema)) {
			try {
				generateDaoOnce(table);
			} catch (Exception e) {
				log.error("Error while generating table DAO " + table, e);
			}
		}

		watch.splitInfo("Child DAOs generated");
	}

	protected void generateDaoOnce(final TableDefinition table) {
		final String className = getExtendedStrategy().getJavaClassName(table, ModeExtended.DAO);

		final File daoFile = getExtendedStrategy().getFile(table, ModeExtended.DAO);
		if (daoFile.exists()) {
			log.info("The child DAO is already generated", className);
		} else {
			log.info("Generating child DAO", getExtendedStrategy().getFileName(table, ModeExtended.DAO));

			final String abstractDao = getExtendedStrategy().getFullJavaClassName(table, Mode.DAO);
			final String beanChild = getExtendedStrategy().getFullJavaClassName(table, ModeExtended.BEAN);
			final String tableIdentifier = getStrategy().getFullJavaIdentifier(table);

			JavaWriter out = new JavaWriter(daoFile);
			printPackage(out, table, ModeExtended.DAO);
			printExtendedClassJavadoc(out, table);

			out.println("public class %s extends %s<%s> {", className, abstractDao, beanChild);

			// Default constructor
			// -------------------
			out.tab(1).javadoc("Create a new %s with an attached configuration", className);
			out.tab(1).println("public %s(%s configuration) {", className, getExtendedStrategy().getSuperDaoConfiguration());
			out.tab(2).println("super(%s, %s.class, configuration);", tableIdentifier, beanChild);
			out.tab(1).println("}");

			out.println("}");
			out.close();
		}
	}

	protected void printPackage(final JavaWriter out, final Definition definition, final ModeExtended mode) {
		out.println("package %s;", getExtendedStrategy().getJavaPackageName(definition, mode));
		out.println();
	}

	protected void printExtendedClassJavadoc(final JavaWriter out, final Definition definition) {
		printExtendedClassJavadoc(out, definition.getComment());
	}

	protected void printExtendedClassJavadoc(final JavaWriter out, final String comment) {
		out.javadoc("This class is generated once by jOOQ Codegen Extended.<br>\n"
				+ " * It will not be overriden by another code generation : you can freely change it.");
	}

	// DAO override

	@Override
	protected void generateDao(final TableDefinition table) {
		final String className = getStrategy().getJavaClassName(table, Mode.DAO);
		final String tableRecord = getStrategy().getFullJavaClassName(table, Mode.RECORD);
		final String daoImpl = getExtendedStrategy().getSuperDao().getName();

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
		out.tab(1).javadoc("{@inheritDoc}");
		out.tab(1).println("protected %s(org.jooq.Table<%s> table, Class<P> type, %s configuration) {", className, tableRecord, getExtendedStrategy().getSuperDaoConfiguration());
		out.tab(2).println("super(table, type, configuration);");
		out.tab(1).println("}");

		// Template method implementations
		// -------------------------------
		out.tab(1).overrideInherit();
		out.tab(1).println("protected %s getId(P object) {", tType);
		out.tab(2).println("return object.%s();", getStrategy().getJavaGetterName(keyColumn, Mode.POJO));
		out.tab(1).println("}");

		out.tab(1).overrideInherit();
		out.tab(1).println("protected void setId(P object, %s id) {", tType);
		out.tab(2).println("object.%s(id);", getStrategy().getJavaSetterName(keyColumn, Mode.POJO));
		out.tab(1).println("}");

		out.tab(1).overrideInherit();
		out.tab(1).println("public boolean isNew(P object) {");
		out.tab(2).println("return getId(object) == null;");
		out.tab(1).println("}");

		out.tab(1).overrideInherit();
		out.tab(1).println("public void save(P object) {");
		out.tab(2).println("if (isNew(object)) {");
		if (getExtendedStrategy().generateId()) {
			out.tab(3).println("setId(object, configurationExtended().idGenerator().generate(%s.class));", tType == Long.class.getName() ? "Long" : "String");
		}
		out.tab(3).println("insert(object);");
		out.tab(2).println("} else {");
		out.tab(3).println("update(object);");
		out.tab(2).println("}");
		out.tab(1).println("}");

		out.tab(1).javadoc("Create a new row query using the configuration defined in the DAO");
		out.tab(1).println("public org.jooq.DSLContext newQuery() {");
		out.tab(2).println("return org.jooq.impl.DSL.using(configuration());");
		out.tab(1).println("}");

		out.tab(1).javadoc("Generate a new select query on the %s table using the configuration defined in the DAO", table.getName());
		out.tab(1).println("public org.jooq.SelectWhereStep<%s> fromTable() {", tableRecord);
		out.tab(2).println("return newQuery().selectFrom(getTable());");
		out.tab(1).println("}");

		for (ColumnDefinition column : table.getColumns()) {
			final String colName = column.getOutputName();
			final String colClass = getExtendedStrategy().getJavaClassName(column, ModeExtended.METHOD);
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
