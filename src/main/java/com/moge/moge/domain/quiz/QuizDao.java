package com.moge.moge.domain.quiz;

import com.moge.moge.domain.quiz.dto.req.PostBoardReq;
import com.moge.moge.domain.quiz.dto.req.PostQuizReq;
import com.moge.moge.domain.quiz.dto.res.PostBoardRes;
import com.moge.moge.domain.quiz.dto.res.PostQuizRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class QuizDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public PostBoardRes createBoard(int userIdx, PostBoardReq postBoardReq) {
        String createBoardQuery = "insert into Board(title, userIdx, categoryIdx) values(?, ?, ?)";
        Object[] createBoardParams = new Object[] {
                postBoardReq.getTitle(),
                userIdx,
                postBoardReq.getCategoryIdx()
        };
        this.jdbcTemplate.update(createBoardQuery, createBoardParams);
        String lastInsertIdQuery = "select last_insert_id()";
        int boardIdx = this.jdbcTemplate.queryForObject(lastInsertIdQuery, int.class);
        return new PostBoardRes(boardIdx, postBoardReq.getCategoryIdx(), userIdx, postBoardReq.getTitle());
    }


    public PostQuizRes createQuiz(PostQuizReq postQuizReq) {
        String createQuizQuery = "insert into Quiz(question, quizType, boardIdx) values(?, ?, ?)";
        System.out.println("post :" + postQuizReq.getQuestion());

        Object[] createQuizParams = new Object[] {
                postQuizReq.getQuestion(),
                postQuizReq.getQuizType(),
                postQuizReq.getBoardIdx()
        };

        this.jdbcTemplate.update(createQuizQuery, createQuizParams);
        String lastInsertIdQuery = "select last_insert_id()";
        int quizIdx = this.jdbcTemplate.queryForObject(lastInsertIdQuery, int.class);
        return new PostQuizRes(postQuizReq.getQuestion(), postQuizReq.getQuizType(), postQuizReq.getBoardIdx(), quizIdx);
    }
}
