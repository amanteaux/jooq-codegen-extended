package org.jooq.util;

import static org.jooq.util.GenerationUtil.convertToJavaIdentifier;

import java.io.File;

import org.jooq.DAO;
import org.jooq.impl.DAOImpl;
import org.jooq.tools.StringUtils;

public class DefaultExtendedGeneratorStrategy extends DefaultGeneratorStrategy implements ExtendedGeneratorStrategy {

	private String childEntitiestargetPackage;

	// default strategy override

	/**
	 * Transform a table name into a java class name. By default transform "camel_case" into "CamelCase".
	 * 
	 * @param tableName
	 * @return The java class name
	 */
	protected String toProperty(final String tableName) {
		return StringUtils.toCamelCase(tableName);
	}

	@Override
	public String getJavaClassName(final Definition definition, final Mode mode) {
		return getJavaClassName0(definition, mode);
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

	// extended

	@Override
	public final void setChildEntitiesTargetDirectory(final String directory) {
		this.childEntitiestargetPackage = directory;
	}

	@Override
	public String getChildEntitiesTargetPackage() {
		return childEntitiestargetPackage;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends DAO> getSuperDao() {
		return DAOImpl.class;
	}

	@Override
	public String getJavaClassName(final Definition definition, final ModeExtended modeExtended) {
		final StringBuilder result = new StringBuilder();

		final String cc = toProperty(definition.getOutputName());
		result.append(GenerationUtil.convertToJavaIdentifier(cc));

		if (modeExtended == ModeExtended.DAO) {
			result.append("Dao");
		}

		return result.toString();
	}

	@Override
	public String getFullJavaClassName(final Definition definition, final ModeExtended mode) {
		StringBuilder sb = new StringBuilder();

		sb.append(getJavaPackageName(definition, mode));
		sb.append(".");
		sb.append(getJavaClassName(definition, mode));

		return sb.toString();
	}

	@Override
	public String getJavaPackageName(final Definition definition, final ModeExtended mode) {
		StringBuilder sb = new StringBuilder();

		sb.append(getChildEntitiesTargetPackage());

		// [#282] In multi-schema setups, the schema name goes into the package <= I trust you on this Lukas :)
		if (definition.getDatabase().getSchemata().size() > 1) {
			sb.append(".");
			sb.append(convertToJavaIdentifier(definition.getSchema().getOutputName()).toLowerCase());
		}

		// Bean are yet in another subpackage
		if (mode == ModeExtended.BEAN) {
			sb.append(".beans");
		}

		// DAOs too
		else if (mode == ModeExtended.DAO) {
			sb.append(".daos");
		}

		return sb.toString();
	}

	@Override
	public File getFile(final Definition definition, final ModeExtended mode) {
		String dir = getTargetDirectory();
		String pkg = getJavaPackageName(definition, mode).replaceAll("\\.", "/");
		return new File(dir + "/" + pkg, getFileName(definition, mode));
	}

	@Override
	public String getFileName(final Definition definition, final ModeExtended mode) {
		return getJavaClassName(definition, mode) + ".java";
	}

}
