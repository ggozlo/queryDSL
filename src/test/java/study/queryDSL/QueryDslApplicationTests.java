package study.queryDSL;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.queryDSL.entity.Hello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit
class QueryDslApplicationTests {

	@Autowired
	//@PersistenceContext
	EntityManager em;

	@Test
	void contextLoads() {
//		Hello hello = new Hello();
//		em.persist(hello);
//
//		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//		QHello qHello = QHello.hello;
//
//		Hello result = queryFactory
//				.selectFrom(qHello)
//				.fetchOne();
//
//		assertThat(result).isEqualTo(hello);
//		assertThat(result.getId()).isEqualTo(hello.getId());
	}
}
