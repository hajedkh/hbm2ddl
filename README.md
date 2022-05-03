#Generic Hibernate jpa2ddl exporter tool (POC)

## Context
In some cases and specially for high availability environments for large applications creating and updating the  schema 
using the automatic tools in hibernate definitely will cause a lot of problems.

StackOverflow question : <a href="https://stackoverflow.com/questions/221379/hibernate-hbm2ddl-auto-update-in-production">
Hibernate: hbm2ddl.auto=update in production?</a>
>No, it's unsafe.
>
>Despite the best efforts of the Hibernate team, you simply cannot rely on automatic updates in production.
> 
>Write your own patches, review them with DBA, test them, then apply them manually.
> 
>Theoretically, if hbm2ddl update worked in development, it should work in production too. But in reality, it's not always the case.
>Even if it worked OK, it may be sub-optimal. DBAs are paid that much for a reason.
> 
> ###### Vladimir Dyuzhev, Software Consultant

As proposed by @Vladimir we need to extract changes and apply it manually to prevent any issue and the most important 
we need to decouple this process from the application server launch to guaranty that it will be created once for all replicas,
in case we are in HA environment example: Kubernetes env,Cloud SAAS...

Solution inspired by: <a href="https://docs.jboss.org/tools/4.1.0.Final/en/hibernatetools/html_single/index.html#d0e4651">
Hibernate-Tools references guide</a> 

## Proof of concept

This is a simple main class project which scans the classpath for annotated entities (Application jpa entities must be loaded in classpath)
from a target package using reflections then it will be mapped to ddl resource executed by hibernate to create the final result 
schema on the target DB.

All target actions are supported except the create-drop because it dependens on the SessionFactory for the  application runtime.

Create a property file and add it as a System property with name PROP_FILE
```
-DPROP_FILE=<path_to_file>
```
Required properties:
```
#hibernate properties, target db to work on 
hibernate.dialect=org.hibernate.dialect.Oracle10gDialect
hibernate.connection.url=jdbc:oracle:thin:@localhost:1521:orcl
hibernate.connection.username=
hibernate.connection.password=

#Target Action
hibernate.hbm2ddl.auto=create

#package to be scanned
package.name=com.myproject.bookstore
```
Add the SGBD driver jar and The entities classes to the classpath and fire it up 🔥

## What's next?

We could launch the extraction in dry-run mode  to generate SQL scripts for the target SGBD (with the target Dialect)
but for me I have developed this solution for an application to be deployed in a Kubernetes cluster which means that
my next step is to containerize it and create a k8s job to be launched before the application main deployment.
