package OOP.Solution;
import OOP.Provided.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class OOPObject {
        private Vector<Object> directParents;
        private Map<String, Object> virtualAncestors;
        static private Map<String ,Object> virtually_constructed = null;

        protected OOPObject() throws OOP4ObjectInstantiationFailedException {
                OOPParent[] OOPp = this.getClass().getAnnotationsByType(OOPParent.class);
                boolean root = false;
                directParents = new Vector<>();
                virtualAncestors = new LinkedHashMap<>();
                Constructor constructor;
                Object o;
                if(virtually_constructed == null ){
                        //only the constructor of the root is initializing the virtually_constructed map
                        root = true;
                        virtually_constructed = new LinkedHashMap<>();
                }
                if (OOPp != null) {
                        //find the virtual classes by DFS search order
                        try{
                                if(root)
                                        findVirtualInheritanceOrder(OOPp);
                                virtualAncestors = virtually_constructed;
                        }
                        catch( IllegalAccessException | InvocationTargetException | InstantiationException| NoSuchMethodException e){
                                virtually_constructed = null;
                                throw new OOP4ObjectInstantiationFailedException();
                        }

                        for( OOPParent parent : OOPp){
                                if (parent.isVirtual()){
                                        directParents.add(virtualAncestors.get(parent.parent().getName()));
                                }
                        }

                        for (OOPParent parent : OOPp) {
                                if (!parent.isVirtual()){
                                        try {
                                                constructor = parent.parent().getDeclaredConstructor();
                                        } catch (NoSuchMethodException e) {
                                                virtually_constructed = null;
                                                throw new OOP4ObjectInstantiationFailedException();
                                        }
                                        //create an instance of parent
                                        try {
                                                if(constructor.toString().contains("protected")){
                                                        constructor.setAccessible(true);
                                                }
                                                o = constructor.newInstance();
                                                if(constructor.toString().contains("protected")){
                                                        constructor.setAccessible(false);
                                                }
                                        } catch (ReflectiveOperationException e) {
                                                virtually_constructed = null;
                                                throw new OOP4ObjectInstantiationFailedException();
                                        }
                                        //add the instance to the ordered collection
                                        directParents.add(o);
                                }
                        }
                }
                //root is the most derived class in the current running constructor(in our mechanism)
                //after the root is constructed: re-set virtually_constructed to null for the next construction
                if(root){
                        virtually_constructed = null;
                }
        }

        //recursively constructing all the virtual inherited classes (but only one instance from each virtual class)
private void findVirtualInheritanceOrder(OOPParent[] OOPp) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(OOPp!=null){
                for(OOPParent parent : OOPp){
                        if(parent.isVirtual() && !virtually_constructed.containsKey(parent.parent().getName()))
                                virtually_constructed.put(parent.parent().getName(),parent.parent().getDeclaredConstructor().newInstance());
                        else{
                                findVirtualInheritanceOrder(parent.parent().getAnnotationsByType(OOPParent.class));
                        }
                }
        }
}

public boolean multInheritsFrom(Class<?> cls) {
                //kind of isKindOf..
                //check if the cls is this class
                if(cls == OOPObject.class) return false;
                if(cls.isInstance(this)){
                        return true;
                }
                //check if one of this class parents (directParents) is cls (recursive)
                for (Object o : directParents) {
                        if(cls.isInstance(o)){
                                return true;
                        }
                        if(o instanceof OOPObject){
                                if (((OOPObject) o).multInheritsFrom(cls)) return true;
                        }
                }
                return false;
        }

        public Object definingObject(String methodName, Class<?>... argTypes)
                throws OOP4AmbiguousMethodException, OOP4NoSuchMethodException {
                //look for the object that can run the method
                try{
                        this.getClass().getMethod(methodName, argTypes);
                        return this;
                }
                catch(NoSuchMethodException ignored2){ }
                Vector<Object> definingParents = new Vector<>();
                for (Object o : directParents) {
                        try {
                                o.getClass().getMethod(methodName, argTypes);
                                definingParents.add(o);
                        }
                        catch (NoSuchMethodException ignored) {
                                if ( o instanceof OOPObject){
                                        try {
                                                Object temp = ((OOPObject) o).definingObject(methodName, argTypes);
                                                if (temp != null) {
                                                        definingParents.add(temp);
                                                }
                                        }catch (OOP4NoSuchMethodException ignored2){
                                        }
                                }
                                else  {
                                        return null;
                                }
                        }
                }
                switch (definingParents.size()) {
                        case 0:
                              throw new OOP4NoSuchMethodException();
                        case 1:
                                return definingParents.elementAt(0);
                        default:
                                //two defining parents - or more
                                //check that all the defining object are the same Object - that means that all the defining parents are virtually inherited
                                for(Object o : definingParents){
                                        if(o!= definingParents.elementAt(0)){
                                                throw new OOP4AmbiguousMethodException();
                                        }
                                }

                                if(virtualAncestors.containsKey(definingParents.elementAt(0).getClass().getName()) ||
                                        inheritedByVirtual(definingParents.elementAt(0))){
                                        return definingParents.elementAt(0);
                                }
                                //they should have the same virtual parent otherwise its ambigous...
                                throw new OOP4AmbiguousMethodException();
                }
        }

        private  boolean inheritedByVirtual(Object obj){
                if(this.virtualAncestors.containsKey(this.getClass().getName())){
                        for(Object o : directParents){
                                if(obj.getClass() == o.getClass()){
                                        return true;
                                }
                        }
                }
                for( Object o : directParents){
                        if(!((OOPObject)o).inheritedByVirtual(obj)){
                                return false;
                        }

                }
                return true;

        }

        public Object invoke(String methodName, Object... callArgs)
                throws OOP4AmbiguousMethodException, OOP4NoSuchMethodException, OOP4MethodInvocationFailedException {
                //the replacement of calling a method in our multiple inheritance functionality
                Object o,ret_o;
                try {
                        Class<?>[] argClasses = new Class<?>[callArgs.length];
                        int i = 0;
                        for (Object obj : callArgs) {
                                argClasses[i] = obj.getClass();
                                i++;
                        }
                        o = this.definingObject(methodName, argClasses);
                        ret_o = o.getClass().getMethod(methodName, argClasses).invoke(o, callArgs);
                } catch (NoSuchMethodException e) {
                        throw new OOP4NoSuchMethodException();
                } catch (RuntimeException | IllegalAccessException | InvocationTargetException ignored) {
                        throw new OOP4MethodInvocationFailedException();
                }
                return ret_o;
        }
}