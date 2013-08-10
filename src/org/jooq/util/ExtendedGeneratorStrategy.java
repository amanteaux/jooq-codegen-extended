package org.jooq.util;

import java.io.File;

import org.jooq.DAO;

public interface ExtendedGeneratorStrategy extends GeneratorStrategy {

	Class<? extends DAO<?, ?, ?>> getSuperDao();

	void setChildEntitiesTargetDirectory(String directory);

	String getChildEntitiesTargetPackage();

	String getFullJavaClassName(Definition definition, ModeExtended mode);

	String getJavaClassName(Definition definition, ModeExtended modeExtended);

	String getJavaPackageName(Definition definition, ModeExtended mode);

	String getFileName(Definition definition, ModeExtended mode);

	File getFile(Definition definition, ModeExtended mode);

	/**
	 * The "mode" by which an artefact should be named
	 */
	enum ModeExtended {

		/**
		 * The default mode. This is used when any {@link Definition}'s meta type is being rendered.
		 */
		BEAN,

		/**
		 * The dao mode. This is used when a {@link TableDefinition}'s dao class is being rendered
		 */
		DAO
	}

}
