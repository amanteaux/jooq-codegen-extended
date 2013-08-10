package org.jooq.util;

import org.jooq.tools.StringUtils;

public class ExtendedGeneratorStrategy extends DefaultGeneratorStrategy {

	@Override
	public String getJavaClassName(final Definition definition, final Mode mode) {
		return getJavaClassName0(definition, mode);
	}

	/**
	 * Transform a table name into a java class name. By default transform "camel_case" into "CamelCase".
	 * 
	 * @param tableName
	 * @return The java class name
	 */
	protected String toProperty(final String tableName) {
		return StringUtils.toCamelCase(tableName);
	}

	private String getJavaClassName0(final Definition definition, final Mode mode) {
		final StringBuilder result = new StringBuilder();

		final String cc = toProperty(definition.getOutputName());
		result.append(GenerationUtil.convertToJavaIdentifier(cc));

		if (mode == Mode.RECORD) {
			result.append("Record");
		} else if (mode == Mode.DAO) {
			result.insert(0, "Abstract");
			result.append("Dao");
		} else if (mode == Mode.INTERFACE) {
			result.insert(0, "I");
		} else if (mode == Mode.POJO) {
			result.append("Pojo");
		} else if (mode == Mode.DEFAULT) {
			result.append("Table");
		}

		return result.toString();
	}

}
