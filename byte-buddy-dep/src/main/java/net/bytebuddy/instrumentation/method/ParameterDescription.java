package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.NamedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.JavaMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Description of the parameter of a Java method or constructor.
 */
public interface ParameterDescription extends AnnotatedElement, NamedElement, ModifierReviewable {

    /**
     * The prefix for names of an unnamed parameter.
     */
    static final String NAME_PREFIX = "arg";

    /**
     * Returns the parameter's type.
     *
     * @return The parameter's type.
     */
    TypeDescription getTypeDescription();

    /**
     * Returns the method that declares this parameter.
     *
     * @return The method that declares this parameter.
     */
    MethodDescription getDeclaringMethod();

    /**
     * Returns this parameter's index.
     *
     * @return The index of this parameter.
     */
    int getIndex();

    /**
     * Checks if this parameter has an explicit name. A parameter without an explicit name is named implicitly by
     * {@code argX} with {@code X} denoting the zero-based index.
     *
     * @return {@code true} if the parameter has an explicit name.
     */
    boolean isNamed();

    /**
     * A base implementation of a method parameter description.
     */
    abstract static class AbstractParameterDescription extends AbstractModifierReviewable implements ParameterDescription {

        @Override
        public String getName() {
            return NAME_PREFIX.concat(String.valueOf(getIndex()));
        }

        @Override
        public int getModifiers() {
            return EMPTY_MASK;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ParameterDescription)) {
                return false;
            }
            ParameterDescription parameterDescription = (ParameterDescription) other;
            return getDeclaringMethod().equals(parameterDescription.getDeclaringMethod())
                    && getIndex() == parameterDescription.getIndex();
        }

        @Override
        public int hashCode() {
            return getDeclaringMethod().hashCode() ^ getIndex();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(Modifier.toString(getModifiers()));
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(isVarArgs()
                    ? getTypeDescription().getName().replaceFirst("\\[\\]$", "...")
                    : getTypeDescription().getName());
            return stringBuilder.append(' ').append(getName()).toString();
        }
    }

    /**
     * Description of a loaded parameter, represented by a Java 8 {@code java.lang.reflect.Parameter}.
     */
    static class ForLoadedParameter extends AbstractParameterDescription {

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getType} method.
         */
        protected static final JavaMethod GET_TYPE;

        /**
         * Initializes the {@link net.bytebuddy.utility.JavaMethod} instances of this class dependant on
         * whether they are available.
         */
        static {
            JavaMethod getName, getDeclaringExecutable, isNamePresent, getModifiers, getDeclaredAnnotations, getType;
            try {
                Class<?> parameterType = Class.forName("java.lang.reflect.Parameter");
                getName = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getName"));
                getDeclaringExecutable = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getDeclaringExecutable"));
                isNamePresent = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("isNamePresent"));
                getModifiers = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getModifiers"));
                getDeclaredAnnotations = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getDeclaredAnnotations"));
                getType = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getType"));
            } catch (Exception ignored) {
                getName = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaringExecutable = JavaMethod.ForUnavailableMethod.INSTANCE;
                isNamePresent = JavaMethod.ForUnavailableMethod.INSTANCE;
                getModifiers = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaredAnnotations = JavaMethod.ForUnavailableMethod.INSTANCE;
                getType = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            GET_NAME = getName;
            GET_DECLARING_EXECUTABLE = getDeclaringExecutable;
            IS_NAME_PRESENT = isNamePresent;
            GET_MODIFIERS = getModifiers;
            GET_DECLARED_ANNOTATIONS = getDeclaredAnnotations;
            GET_TYPE = getType;
        }

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getName} method.
         */
        private static final JavaMethod GET_NAME;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s
         * {@code getDeclaringExecutable} method.
         */
        private static final JavaMethod GET_DECLARING_EXECUTABLE;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code isNamePresent} method.
         */
        private static final JavaMethod IS_NAME_PRESENT;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getModifiers} method.
         */
        private static final JavaMethod GET_MODIFIERS;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getDeclaredAnnotations}
         * method.
         */
        private static final JavaMethod GET_DECLARED_ANNOTATIONS;

        /**
         * An instance of {@code java.lang.reflect.Parameter}.
         */
        private final Object parameter;

        /**
         * The parameter's index.
         */
        private final int index;

        /**
         * Creates a representation of a loaded parameter.
         *
         * @param parameter An instance of {@code java.lang.reflect.Parameter}.
         * @param index     The parameter's index.
         */
        protected ForLoadedParameter(Object parameter, int index) {
            this.parameter = parameter;
            this.index = index;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return new TypeDescription.ForLoadedType((Class<?>) GET_TYPE.invoke(parameter));
        }

        @Override
        public MethodDescription getDeclaringMethod() {
            Object executable = GET_DECLARING_EXECUTABLE.invoke(parameter);
            if (executable instanceof Method) {
                return new MethodDescription.ForLoadedMethod((Method) executable);
            } else if (executable instanceof Constructor) {
                return new MethodDescription.ForLoadedConstructor((Constructor<?>) executable);
            } else {
                throw new IllegalStateException("Unknown executable type: " + executable);
            }
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation((Annotation[]) GET_DECLARED_ANNOTATIONS.invoke(parameter));
        }

        @Override
        public String getName() {
            return (String) GET_NAME.invoke(parameter);
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isNamed() {
            return (Boolean) IS_NAME_PRESENT.invoke(parameter);
        }

        @Override
        public int getModifiers() {
            return (Integer) GET_MODIFIERS.invoke(parameter);
        }

        /**
         * Description of a loaded method's parameter on a virtual machine where {@code java.lang.reflect.Parameter}
         * is not available.
         */
        protected static class OfLegacyVmMethod extends AbstractParameterDescription {

            /**
             * The method that declares this parameter.
             */
            private final Method method;

            /**
             * The index of this parameter.
             */
            private final int index;

            /**
             * The type of this parameter.
             */
            private final Class<?> parameterType;

            /**
             * The annotations of this parameter.
             */
            private final Annotation[] parameterAnnotation;

            /**
             * Creates a legacy representation of a method's parameter.
             *
             * @param method              The method that declares this parameter.
             * @param index               The index of this parameter.
             * @param parameterType       The type of this parameter.
             * @param parameterAnnotation The annotations of this parameter.
             */
            protected OfLegacyVmMethod(Method method, int index, Class<?> parameterType, Annotation[] parameterAnnotation) {
                this.method = method;
                this.index = index;
                this.parameterType = parameterType;
                this.parameterAnnotation = parameterAnnotation;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return new TypeDescription.ForLoadedType(parameterType);
            }

            @Override
            public MethodDescription getDeclaringMethod() {
                return new MethodDescription.ForLoadedMethod(method);
            }

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public boolean isNamed() {
                return false;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.ForLoadedAnnotation(parameterAnnotation);
            }
        }

        /**
         * Description of a loaded constructor's parameter on a virtual machine where {@code java.lang.reflect.Parameter}
         * is not available.
         */
        protected static class OfLegacyVmConstructor extends AbstractParameterDescription {

            /**
             * The method that declares this parameter.
             */
            private final Constructor<?> constructor;

            /**
             * The index of this parameter.
             */
            private final int index;

            /**
             * The type of this parameter.
             */
            private final Class<?> parameterType;

            /**
             * The annotations of this parameter.
             */
            private final Annotation[] parameterAnnotation;

            /**
             * Creates a legacy representation of a method's parameter.
             *
             * @param constructor         The constructor that declares this parameter.
             * @param index               The index of this parameter.
             * @param parameterType       The type of this parameter.
             * @param parameterAnnotation The annotations of this parameter.
             */
            protected OfLegacyVmConstructor(Constructor<?> constructor, int index, Class<?> parameterType, Annotation[] parameterAnnotation) {
                this.constructor = constructor;
                this.index = index;
                this.parameterType = parameterType;
                this.parameterAnnotation = parameterAnnotation;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return new TypeDescription.ForLoadedType(parameterType);
            }

            @Override
            public MethodDescription getDeclaringMethod() {
                return new MethodDescription.ForLoadedConstructor(constructor);
            }

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public boolean isNamed() {
                return false;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.ForLoadedAnnotation(parameterAnnotation);
            }
        }
    }

    /**
     * A latent description of a parameter that is not attached to a method or constructor.
     */
    static class Latent extends AbstractParameterDescription {

        /**
         * The method that is declaring the parameter.
         */
        private final MethodDescription declaringMethod;

        /**
         * The type of the parameter.
         */
        private final TypeDescription parameterType;

        /**
         * The index of the parameter.
         */
        private final int index;

        /**
         * The annotations of the parameter.
         */
        private final List<? extends AnnotationDescription> parameterAnnotations;

        /**
         * Creates a latent description of a parameter.
         *
         * @param declaringMethod      The method that is declaring the parameter.
         * @param parameterType        The type of the parameter.
         * @param index                The index of the parameter.
         * @param parameterAnnotations The annotations of the parameter.
         */
        public Latent(MethodDescription declaringMethod,
                      TypeDescription parameterType,
                      int index,
                      List<? extends AnnotationDescription> parameterAnnotations) {
            this.declaringMethod = declaringMethod;
            this.parameterType = parameterType;
            this.index = index;
            this.parameterAnnotations = parameterAnnotations;
        }

        @Override
        public TypeDescription getTypeDescription() {
            return parameterType;
        }

        @Override
        public MethodDescription getDeclaringMethod() {
            return declaringMethod;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(parameterAnnotations);
        }
    }
}