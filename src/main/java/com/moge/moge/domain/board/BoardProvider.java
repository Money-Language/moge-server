package com.moge.moge.domain.board;

import com.moge.moge.domain.board.model.res.GetBoardCommentRes;
import com.moge.moge.global.common.BaseResponse;
import com.moge.moge.global.config.security.JwtService;
import com.moge.moge.global.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.moge.moge.global.exception.BaseResponseStatus.BOARD_NOT_EXISTS;
import static com.moge.moge.global.exception.BaseResponseStatus.DATABASE_ERROR;

@Service
public class BoardProvider {

    private final BoardDao boardDao;
    private final JwtService jwtService;

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public BoardProvider(BoardDao boardDao, JwtService jwtService) {
        this.boardDao = boardDao;
        this.jwtService = jwtService;
    }

    public List<GetBoardCommentRes> getBoardComments(int boardIdx) throws BaseException {
        //try {
            if (boardDao.checkBoardExists(boardIdx) == 0) {
                throw new BaseException(BOARD_NOT_EXISTS);
            }
            return boardDao.getBoardComments(boardIdx);

        //} catch (BaseException exception) {
        //    throw new BaseException(DATABASE_ERROR);
        //}
    }
}
