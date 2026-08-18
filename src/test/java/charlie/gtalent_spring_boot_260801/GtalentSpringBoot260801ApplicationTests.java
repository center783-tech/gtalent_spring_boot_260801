package charlie.gtalent_spring_boot_260801;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestMailConfig.class)
class GtalentSpringBoot260801ApplicationTests {

	@Test
	void contextLoads() {
	}

}
