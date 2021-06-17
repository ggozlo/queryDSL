package study.queryDSL.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.queryDSL.entity.Member;
import study.queryDSL.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local") // 프로퍼티 파일 한정
@Component
@RequiredArgsConstructor
public class initMember {

    private final initMemberService init;

    @PostConstruct // 생애주기상 Post 와 트랜잭션 분리
    public void init() {
         init.init();
    }

    @Component
    static class initMemberService {
        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init() {
            Team teamA =new Team("teamA");
            Team teamB =new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i%2 == 0? teamA : teamB;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }

}
