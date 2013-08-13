package org.jooq.util;

import java.io.File;

import org.jooq.DAO;

/**
 * 
 * The extended naming strategy for the {@link JavaExtendedGenerator}
 * 
 * @author Aurélien Manteaux
 * 
 */
public interface ExtendedGeneratorStrategy extends GeneratorStrategy {

	@SuppressWarnings("rawtypes")
	Class<? extends DAO> getSuperDao();

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
		 * This is used when any {@link Definition}'s bean is being rendered.
		 */
		BEAN,

		/**
		 * This is used when a {@link TableDefinition}'s dao child class is being rendered
		 */
		DAO,

		/**
		 * This is used when a {@link TableDefinition}'s field is rendered into a method like fetchField
		 */
		METHOD,
	}

}
