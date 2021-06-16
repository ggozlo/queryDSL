package study.queryDSL.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age" } ) // toString의 롬복으로 특정 필드만 지정해서 생성, 양방향 연관관계는 하면 무한루프 가능
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;



    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if(team != null) {
            changeTeam(team);
        }
    }

    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }


    public Member(String username, int age) {
        this(username, age, null); // 자신의 생성자를 호출하는 생성자...
    }

    public Member(String username) {
        this(username, 0);
    }
}
