package io.jpa2ddl.launcher;


import org.hibernate.JDBCException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.reflections.Reflections;


import javax.persistence.Entity;
import java.io.FileInputStream;
import java.util.*;

import static org.hibernate.bytecode.BytecodeLogger.LOGGER;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;


/**
 * @author hajedkh
 *
 *
 * Simple implementation of the hbm2ddl Hibernate-tool.
 * This main class is an externalization of the "hibernate.hbm2ddl.auto" process
 * which exports the schema from annotated entities loaded in the classPath
 * (filtred with package name) to ddl.
 *
 *
 * @see <a href="https://docs.jboss.org/tools/4.1.0.Final/en/hibernatetools/html_single/index.html#d0e4651">
 *     Database schema exporter (<hbm2ddl>)</a>
 *
 *
 * TODO (0): Create a separated configuration class, add H2 as default DB, add unit tests.
 * TODO (1): Add more options to the Reflections scanner for multiple modules and multiple criterias.
 * TODO (3): Add dry-run option using the TargetType "SCRIPT" to generate sql scripts only.
 *
 */
public  class SchemaToolLauncher {

    public static void main(String[] args) throws Exception {

        if(System.getProperty("PROP_FILE")!= null) {

            //Import properties file from command line
            Properties prop = new Properties();
            prop.load(new FileInputStream(System.getProperty("PROP_FILE")));

            // configure hibernate-tool Metadata
            MetadataSources metadata = new MetadataSources(
                    new StandardServiceRegistryBuilder()
                            .applySettings(prop)
                            .build());

            //Load annotated classes "@Entity" from target package in current classPath
            Reflections reflections = new Reflections(prop.getProperty("package.name"));
            Set<Class<?>> annotated = reflections.get(SubTypes.of(TypesAnnotated.with(Entity.class)).asClass());

            // add annotated classes to the schemaExport MetadataSources
            annotated.forEach(metadata::addAnnotatedClass);



            //Launch Target Action
            switch (prop.getProperty("hibernate.hbm2ddl.auto").toLowerCase()) {
                case "drop":
                    executeSchemaExport(metadata, SchemaExport.Action.DROP);
                    break;
                case "create-only":
                    executeSchemaExport(metadata, SchemaExport.Action.CREATE);
                    break;
                case "create":
                    executeSchemaExport(metadata, SchemaExport.Action.BOTH);
                    break;
                case "create-drop":
                    LOGGER.warn("Could not proceed with create-drop process with hibernate tools in  standalone mode," +
                            "This action depends on the SessionFactory status at runtime," +
                            " proceeding with create");
                    executeSchemaExport(metadata, SchemaExport.Action.BOTH);
                    break;
                case "update":
                    executeSchemaUpdate(metadata);
                    break;
                default:
                    LOGGER.info("No hibernate ddl auto creation process.");
            }
        }else{
            LOGGER.error("No properties file specified");
        }
    }

    /**
     * Schema export process definition.
     * Target actions : [create,create-only,drop]
     *
     * @param metadata
     * @param schemaExportAction
     */
    private static void executeSchemaExport(MetadataSources metadata, SchemaExport.Action schemaExportAction)
            throws Exception {
        SchemaExport export = new SchemaExport();
        try {
            export.execute(EnumSet.of(TargetType.DATABASE), schemaExportAction, metadata.buildMetadata());
        }catch (JDBCException jdbcException){
            throw  new Exception(jdbcException);
        }
    }

    /**
     * Schema update process definition.
     * Specific process for hibernate schemaExport [update]
     *
     * @param metadata
     */
    private static void executeSchemaUpdate(MetadataSources metadata) throws Exception {
        SchemaUpdate schemaUpdate = new SchemaUpdate();
        try {
            schemaUpdate.execute(EnumSet.of(TargetType.DATABASE), metadata.buildMetadata());
        }catch (JDBCException jdbcException){
            throw new Exception(jdbcException);
        }
    }
}
