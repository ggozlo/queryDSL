package study.queryDSL;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.queryDSL.dto.MemberDto;
import study.queryDSL.dto.QMemberDto;
import study.queryDSL.dto.UserDto;
import study.queryDSL.entity.Member;
import study.queryDSL.entity.QMember;
import study.queryDSL.entity.QTeam;
import study.queryDSL.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.queryDSL.entity.QMember.*;
import static study.queryDSL.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;
    @BeforeEach // 개별 테스트 이전에 실행
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member finfMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(finfMember.getUsername()).isEqualTo("member1");
        //then
    }

    @Test
    public void startQuerydsl() {

        //QMember m = new QMember("m"); // 같은 테이블 조인할 때만 직접 선언해서 사용

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        //member.username.eq("member1").and(member.age.eq(10))
                        member.username.eq("member1"),(member.age.eq(10)) // and
                )
                .fetchOne();
            /*
            *   eq : equal, =
            *   ne : not equal, !=, .eq(x).not()
            *   .isNotNull
            *   .in()
            *  .notIn()
            *  .between()
            *  .goe() >= , .gt() > , .loe() <= , .lt() <
            *  .like() % , .contains() %~% , startWith ~%
            * */
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        //given
//        List<Member> fetch = queryFactory.selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // 페이징 쿼리, 카운트 쿼리도 같이 보냄, 성능이 중요하면 카운트 쿼리는 따로쓰기 - 조인쿼리에선 성능저하

        results.getTotal(); // 요소의 총계
        List<Member> content = results.getResults(); // 요소 본문

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 카운트 쿼리

        //then
    }

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // null이 마지막으로, 복수조건 정렬엔 콤마 로 구분
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 시작 위치 - 최소는 0
                .limit(2) // 가져올 최대 갯수
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(), // 각종 집계함수도 사용 가능
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch(); // 복수의 데이터 타입을 반환할땐 튜플로 반환

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4); // 튜플에서 값을 획득할려면 select 절에 쓴 집계함수를 넣으면 값이 나옴
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name) // 그룹핑 가능능
                 .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // 조인대상, 약칭
                //  기본은 inner 조인, leftjoin() 등은 따로 있음
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    // 세타 조인
    // 회원 이름이 팀 이름과 같은 회원 조회
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // 세타조인, from 절에 대상 테이블을 둘다 기입, 카테이션곱 으로 쿼리문을 짜서 조건에 맞게 가져옴

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    // 회원과 팀을 조인하면 팀 이름은 teamA, 회원은 모두 조회회
    // jpql : select m ,t from Member m left join m.team t on t.name = 'teamA'
   @Test
    public void join_in_filtering() {
       List<Tuple> result = queryFactory
               .select(member, team)
               .from(member)
               .leftJoin(member.team, team).on(team.name.eq("teamA")) // 외부조인은 on으로 해야함
//               .join(member.team, team).on(team.name.eq("teamA"))
//               .join(member.team, team).where(team.name.eq("teamA")) 내부 조인은 where로 일치조건 가능
               .fetch();

       for (Tuple tuple : result) {
           System.out.println("tuple = " + tuple);
       }

   }

   // 연관관계 없는 엔티티 외부조인
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // 연관관계 에 속한 테이블이 아닌 테이블 자체를  조인
                .on(member.username.eq(team.name)) // on절로 조인 조건을 지정
                .fetch();


        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    @PersistenceUnit // 엔티티 매니저 팩토리를 획득 하기 위한 어노테이션
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 초기화가 된 엔티티 인지 아니면 안된 (프록시인?) 엔티티 인지
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 조인은 똑같이 쓰고 체이닝기법으로 fetch 조인임을 지정
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 초기화가 된 엔티티 인지 아니면 안된 (프록시인?) 엔티티 인지
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    // 나이가 가장 많은
    // 나이가 평균 이상인
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 별칭은 다르게 써야함

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
        assertThat(result2).extracting("age").containsExactly(30,40);
    }

    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 별칭은 다르게 써야함

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in( // 서브쿼리에서 반환된 나이들과 비교하여 필터링
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) // 서브쿼리에서 나이가 10보다 큰 데이터들을 찾아내서 반환
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20,30,40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");


        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions // 스태틱 임포트 가능
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member) // jpa 는 from 절의 서브쿼리가 안됨, 써야한다면 다른 jdbc 구현체를 쓸것
                .fetch();

        //then
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder() // 복잡한 케이스문 을 생성 하기 위한 caseBuilder
                        .when(member.age.between(0, 20)).then("0~20살살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) // Expressions 클래스를 통한 상수 추력, 쿼리문에는 포함되지 않느다
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // 문자열로 변환하여 문자열 더하기
                .from(member)
                .fetch();
        // .stringValue - Enum 타입 처리할때 유용하다
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 중급문법 ----------------------------------------------------------------------------------------------------

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory // dsl 의 tuple 타입 - 적절한 프로젝션으로 필요한 타입만 가져온다
                .select(member.username, member.age)
                .from(member)
                .fetch();
        // tuple 은 repository 또는 DAO 단을 넘는것 좋지 않음

        for (Tuple tuple : result) {
            String username = tuple.get(member.username); // 튜플에서 원하는 값 추출하기기
           Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                "select new study.queryDSL.dto.MemberDto(m.username,m.age) from Member m"
                , MemberDto.class).getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        // dsl 의 dto 생성방식 3가지, 생성자, 필드 직접접근, setter

        List<MemberDto> result = queryFactory
                .select(
                        Projections.bean( // setter 방식
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByFields() {
        // dsl 의 dto 생성방식 3가지, 생성자, 필드 직접접근, setter

        List<MemberDto> result = queryFactory
                .select(
                        Projections.fields( // 필드에 직접 입력, private 무시
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        // dsl 의 dto 생성방식 3가지, 생성자, 필드 직접접근, setter

        List<MemberDto> result = queryFactory
                .select(
                        Projections.constructor( // 필드에 직접 입력 /  타입, 순서에 맞게 입력해야함
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
        // dsl 의 dto 생성방식 3가지, 생성자, 필드 직접접근, setter
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(
                        Projections.fields( // 필드에 직접 입력, private 무시, setter, 필드 방식은 필드명과 변수명이 같아야 한다
                                UserDto.class,
                                member.username.as("name"),

                                ExpressionUtils.as(JPAExpressions
                                    .select(memberSub.age.max())
                                        .from(memberSub), "age") // select 절에서 서브쿼리에 약칭을 지정하고자 할때는 ExpressionUtils를 사용
                        ))
               .from(member)
                .fetch();
        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findByQueryProjection() {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() { // 불린 빌더를 이용한 동적 쿼리
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result= searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameParam)); // 불린 빌더 생성, 초기값 주입 가능

        if(usernameParam != null) { // 파라미터가 null 인지 확인
            builder.and(member.username.eq(usernameParam));  // null 이 아니라면 빌더에 조건절 쿼리문 추가
        } // null 이라면 해당 조건 추가 안함

        if(ageParam != null) { // 2번째
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) // 조건에 맞게 작성된 빌더의 쿼리를 dsl에 적용, builder에도 추가적인 조건 가능
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() { // where 절에 메서드 사용
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result= searchMember2(usernameParam, ageParam);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where( allEq(usernameParam, ageParam) ) // where 절에 메서드 호출, null을 받으면 쿼리문에 반영 안됨
                .fetch();
    }
    // 메서드들은 재활용 가능, 가독성 상승
    private BooleanExpression ageEq(Integer ageParam) { // where절에 호출된 동적 조건 메서드
        // 값이 있다면 조건을 부여
        // 받은 파라메터가 null 이면 그대로 null 반환
        return ageParam != null ? member.age.eq(ageParam) : null;
    }
    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }
    private BooleanExpression allEq(String username, Integer age) { // 메서드들을 메서드 하나로 묶어주는 메서드
        return ageEq(age).and(usernameEq(username)); // null 처리는 따로 해줘야함..
    }

    @Test
    public void bulkUpdate() { // 업데이트
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원").set(member.age, 30)
                .where(member.age.lt(28))
                .execute(); // 벌크연산은 영속성 컨텍스트를 무시하고  db에 쿼리문을 보낸다 즉 둘 사이의 값이 달라질수 있다.
                // jpa는 기존 영속성 컨텍스트 와 db에서 끌어온 데이터의 id가 같다면 영속성 컨텍스트를 우선한다
        em.flush();
        em.clear();

        List<Member> fetch = queryFactory.select(member).from(member).fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    public void bulkadd() { // 사칙연산
        queryFactory
                .update(member)
                .set(member.age, member.age.divide(2)) // add() , 뺴기는 없음 -로 할것, 곱셈은 multiply, 나누기는 divide
                .execute();
    }

    @Test
    public void bulkDelete() { // 삭제
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() { // sql 함수 호출
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate( // Expressions 를 사용 정수를 건드릴 거면 intTemplate, 함수가 Dialect 에 등록이 되어있어야 함
                                "function('replace',{0},{1},{2})", member.username,"member", "m"))
                //sql의 replace 함수 를 호출, 스펙상의 인덱스를 사용할 범위만큼 호출, 이후 순서대로 열거
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower',{0} )", member.username)))
                .where(member.username.eq(member.username.lower())) // 표준 함수들은 대부분 dsl에서 지원함
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
}
