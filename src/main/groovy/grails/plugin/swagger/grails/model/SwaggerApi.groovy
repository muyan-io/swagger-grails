package grails.plugin.swagger.grails.model

import com.fasterxml.jackson.annotation.JsonIgnore
import grails.plugin.swagger.grails.SwaggerBuilderHelper
import grails.plugin.swagger.grails.SwaggerController
import grails.util.Holders
import groovy.transform.ToString
import io.swagger.annotations.Api
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.grails.core.DefaultGrailsControllerClass

/**
 * Contains the information that allows swagger generation for controller
 * and action level annotations.
 */
@ToString(includes = ['className', 'tag', 'shortName'], includePackage = false)
class SwaggerApi implements SwaggerBuilderHelper {
    String className
    String tag
    String shortName
    List<SwaggerOperation> swaggerOperations = []

    SwaggerApi(DefaultGrailsControllerClass controllerClass) {
        this.className = controllerClass.clazz.name
        this.tag = controllerClass.naturalName
        this.shortName = controllerClass.shortName
        this.swaggerOperations = controllerClass.actions.collect { String actionName ->
            new SwaggerOperation(controllerClass, actionName)
        }.sort {
            it.value
        }
    }

    /**
     * Build the api annotation that is attached to the controller at the class level.
     *
     * @param constPool {@link javassist.bytecode.ConstPool}
     * @return {@Link AnnotationsAttribute} to attach back to the class file
     */
    AnnotationsAttribute buildApiAnnotation(ConstPool constPool) {
        AnnotationsAttribute attribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
        Annotation annotation = new Annotation(Api.class.name, constPool)

        annotation.addMemberValue("value", new StringMemberValue("/", constPool))

        ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool)
        arrayMemberValue.setValue([new StringMemberValue(tag.toString(), constPool)] as StringMemberValue[])
        annotation.addMemberValue("tags", arrayMemberValue)

        attribute.addAnnotation(annotation)
        attribute
    }

    /**
     * Builds a list of SwaggerApis from all available Grails controllers
     *
     * @return List of {@link SwaggerApi} objects
     */
    @JsonIgnore
    static List<SwaggerApi> getApis() {
        Holders.grailsApplication["controllerClasses"].findAll { DefaultGrailsControllerClass it ->
            it.clazz.name != SwaggerController.class.name
        }.collect { DefaultGrailsControllerClass it ->
            new SwaggerApi(it)
        }.sort {
            it.tag
        }
    }
}
