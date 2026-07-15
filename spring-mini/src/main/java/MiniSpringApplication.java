import com.demo.service.AppConfig;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@NullUnmarked
@ComponentScan
@Configuration
@EnableAspectJAutoProxy //创建代理
public class MiniSpringApplication {
	public static void main(String[] args) {
		System.out.printf("Hello and welcome!\n");
		AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext(AppConfig.class);
		System.out.println(context.getBean("userService"));
		System.out.println(context.getBean("userService"));
		System.out.println(context.getBean("userService"));
	}
}