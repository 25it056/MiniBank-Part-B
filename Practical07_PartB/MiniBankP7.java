import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.*;

public class MiniBankP7 {
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface Id {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface Positive { String message() default "must be > 0"; }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface MaxLength { int value(); }
    static class Account {
        @Id private String accountNumber;
        @Positive private long balance;
        @MaxLength(20) private String ownerName;
        Account(String no,long balance,String owner){accountNumber=no;this.balance=balance;ownerName=owner;}
    }
    static class AnnotationValidator {
        static String[] validate(Object obj){
            List<String> errors=new ArrayList<>();
            for(Field f:obj.getClass().getDeclaredFields()){
                try{
                    f.setAccessible(true); Object value=f.get(obj);
                    if(f.isAnnotationPresent(Positive.class) && value instanceof Number n && n.longValue()<=0)
                        errors.add(f.getName()+" "+f.getAnnotation(Positive.class).message());
                    if(f.isAnnotationPresent(MaxLength.class) && value instanceof String s && s.length()>f.getAnnotation(MaxLength.class).value())
                        errors.add(f.getName()+" length must be <= "+f.getAnnotation(MaxLength.class).value());
                }catch(IllegalAccessException e){errors.add("Cannot access "+f.getName());}
            }
            return errors.toArray(new String[0]);
        }
    }
    public static void main(String[] args){
        Account a=new Account("AC0001",-500,"A very long customer name exceeding limit");
        System.out.println("Validation errors:");
        for(String e:AnnotationValidator.validate(a))System.out.println("- "+e);
    }
}
