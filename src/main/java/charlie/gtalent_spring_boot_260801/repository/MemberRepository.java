package charlie.gtalent_spring_boot_260801.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import charlie.gtalent_spring_boot_260801.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    
    @Query(
        value="SELECT COUNT(*) FROM members WHERE account = :account",
                nativeQuery = true
    )
    // 去資料庫內搜尋這個帳號的數量
    public long countByAccount(@Param("account") String account);


    @Query(
        value="SELECT * FROM members WHERE id = :id and status = 1",
                nativeQuery = true
    )
    // 找出單一會員By Id
    public Optional<Member> findOneById(@Param("id") long id);

    

    // 會員登入用 account 查詢，status = 1 才能登入。
    @Query(
            value = "SELECT * FROM members WHERE account = :account AND status = 1",
            nativeQuery = true
    )
    public Optional<Member> findOneByAccountAndStatus(
            @Param("account") String account
    );

    @Query(
        value = "SELECT * FROM members WHERE status = 1",
        countQuery = "SELECT count(*) FROM members WHERE status = 1",
        nativeQuery = true
    )
    Page<Member> findAllActive(Pageable pageable);

    List<Member> findByAccountIn(List<String> accounts);

    // 會員忘記密碼用 account or email 查詢，status = 1 才能登入。
    @Query(
            value = "SELECT * FROM members WHERE (account = :accountOrEmail OR email = :accountOrEmail) AND status = 1",
            nativeQuery = true
    )
    public Optional<Member> findOneByAccountOrEmailAndStatus(
            @Param("accountOrEmail") String accountOrEmail
    );

    @Query("""
        SELECT m
        FROM Member m
        WHERE (:account IS NULL OR m.account LIKE CONCAT('%', :account, '%'))
        AND (:name IS NULL OR m.name LIKE CONCAT('%', :name, '%'))
        AND (:gender IS NULL OR m.gender = :gender)
        AND (:status IS NULL OR m.status = :status)
        """)
        List<Member> search(
                @Param("account") String account,
                @Param("name") String name,
                @Param("gender") Byte gender,
                @Param("status") Byte status
        );

    
}
