package com.bloodDonation.board.service;

import com.bloodDonation.board.controllers.BoardDataSearch;
import com.bloodDonation.board.controllers.RequestBoard;
import com.bloodDonation.board.entities.*;
import com.bloodDonation.board.entities.QBoardData;
import com.bloodDonation.board.repositories.BoardDataRepository;
import com.bloodDonation.board.repositories.BoardViewRepository;
import com.bloodDonation.board.service.comment.CommentInfoService;
import com.bloodDonation.board.service.config.BoardConfigInfoService;
import com.bloodDonation.commons.ListData;
import com.bloodDonation.commons.Pagination;
import com.bloodDonation.commons.Utils;
import com.bloodDonation.file.entities.FileInfo;
import com.bloodDonation.file.service.FileInfoService;
import com.bloodDonation.member.Authority;
import com.bloodDonation.member.MemberUtil;
import com.bloodDonation.member.entities.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardInfoService {

    private final EntityManager em;
    private final BoardDataRepository boardDataRepository;
    private final BoardViewRepository boardViewRepository;

    private final BoardConfigInfoService configInfoService;
    private final CommentInfoService commentInfoService;

    private final FileInfoService fileInfoService;
    private final HttpServletRequest request;

    private final MemberUtil memberUtil;
    private final Utils utils;

    /**
     * 게시글 조회
     *
     * @param seq : 게시글 번호
     * @return
     */
    public BoardData get(Long seq) {
        BoardData boardData = boardDataRepository.findById(seq).orElseThrow(BoardDataNotFoundException::new);

        addBoardData(boardData);

        // 댓글 목록
        List<CommentData> comments = commentInfoService.getList(seq);
        boardData.setComments(comments);

        return boardData;
    }

    /**
     * BoardData -> RequestBoard
     * @param data : 게시글 데이터(BoardData), 게시글 번호(Long)
     * @return
     */
    public RequestBoard getForm(Object data) {
        BoardData boardData = null;
        if (data instanceof BoardData) {
            boardData = (BoardData) data;
        } else {
            Long seq = (Long) data;
            boardData = get(seq);
        }

        RequestBoard form = new ModelMapper().map(boardData, RequestBoard.class);
        form.setMode("update");
        form.setBid(boardData.getBoard().getBid());

        return form;
    }

    /**
     * 특정 게시판 목록을 조회
     *
     * @param bid : 게시판 ID
     * @param search
     * @return
     */
    public ListData<BoardData> getList(String bid, BoardDataSearch search) {

        Board board = StringUtils.hasText(bid) ? configInfoService.get(bid) : new Board();


        board = configInfoService.get(bid);

        int page = Utils.onlyPositiveNumber(search.getPage(), 1);
        int limit = Utils.onlyPositiveNumber(search.getLimit(), board.getRowsPerPage());
        int offset = (page - 1) * limit; // 레코드 시작 위치

        QBoardData boardData = QBoardData.boardData;
        BooleanBuilder andBuilder = new BooleanBuilder();
        if (StringUtils.hasText(bid)){
            andBuilder.and(boardData.board.bid.eq(bid)); // 게시판 ID
        }
        /* 검색 조건 처리 S */

        String sopt = search.getSopt();
        String skey = search.getSkey();

        sopt = StringUtils.hasText(sopt) ? sopt.toUpperCase() : "ALL";

        if (StringUtils.hasText(skey)) {
            skey = skey.trim();

            BooleanExpression subjectCond = boardData.subject.contains(skey); // 제목 - subject LIKE '%skey%';
            BooleanExpression contentCond = boardData.content.contains(skey); // 내용 - content LIKE '%skey%';

            if (sopt.equals("SUBJECT")) { // 제목

                andBuilder.and(subjectCond);

            } else if (sopt.equals("CONTENT")) { // 내용

                andBuilder.and(contentCond);

            } else if (sopt.equals("SUBJECT_CONTENT")) { // 제목 + 내용

                BooleanBuilder orBuilder = new BooleanBuilder();
                orBuilder.or(subjectCond)
                        .or(contentCond);

                andBuilder.and(orBuilder);

            } else if (sopt.equals("POSTER")) { // 작성자 + 아이디 + 회원명
                BooleanBuilder orBuilder = new BooleanBuilder();
                orBuilder.or(boardData.poster.contains(skey))
                        .or(boardData.member.userId.contains(skey))
                        .or(boardData.member.mName.contains(skey));

                andBuilder.and(orBuilder);
            }

        }

        // 특정 사용자로 게시글 한정 : 마이페이지에서 활용 가능
        String userId = search.getUserId();
        if (StringUtils.hasText(userId)) {
            andBuilder.and(boardData.member.userId.eq(userId));
        }

        // 게시글 분류 조회
        String category = search.getCategory();
        if (StringUtils.hasText(category)) {
            category = category.trim();
            andBuilder.and(boardData.category.eq(category));
        }

        /* 검색 조건 처리 E */

        PathBuilder<BoardData> pathBuilder = new PathBuilder<>(BoardData.class, "boardData");

        List<BoardData> items = new JPAQueryFactory(em)
                .selectFrom(boardData)
                .leftJoin(boardData.member)
                .fetchJoin()
                .offset(offset)
                .limit(limit)
                .where(andBuilder)
                .orderBy(
                        new OrderSpecifier(Order.DESC, pathBuilder.get("notice")),
                        new OrderSpecifier(Order.DESC, pathBuilder.get("listOrder")),
                        new OrderSpecifier(Order.ASC, pathBuilder.get("listOrder2")),
                        new OrderSpecifier(Order.DESC, pathBuilder.get("createdAt"))
                )
                .fetch();

        // 게시글 전체 갯수
        long total = boardDataRepository.count(andBuilder);

        int ranges = utils.isMobile() ? board.getPageCountMobile() : board.getPageCountPc();

        Pagination pagination = new Pagination(page, (int)total, ranges, limit, request);

        return new ListData<>(items, pagination);
    }

    public ListData<BoardData> getList(BoardDataSearch search) {
        return getList(null, search);
    }
    /**
     * 최신 게시글
     * @param bid : 게시판 아이디
     * @param limit :  조회할 갯수
     * @return
     */
    public List<BoardData> getLatest(String bid, int limit) {
        BoardDataSearch search = new BoardDataSearch();
        search.setLimit(limit);
        ListData<BoardData> data = getList(bid,search);

        return data.getItems();
    }

    public List<BoardData> getLatest(String bid) {
        return getLatest(bid,10);
    }

    /**
     * 게시글 추가 정보 처리
     *
     * @param boardData
     */
    public void addBoardData(BoardData boardData) {
        /* 파일 정보 추가 s */
        String gid = boardData.getGid();

        List<FileInfo> editorFiles = fileInfoService.getListDone(gid, "editor");
        List<FileInfo> attachFiles = fileInfoService.getListDone(gid, "attach");

        boardData.setEditorFiles(editorFiles);
        boardData.setAttachFiles(attachFiles);
        /* 파일 정보 추가 E*/

        /* 수정, 삭제 권한 정보 S */
        boolean editable = false, deletable = false, mine = false;//mine : 내껀지
        Member _member = boardData.getMember(); // null- 비회원, X null -> 회원

        // 관리자 -> 삭제 수정 모두 가능
        if (memberUtil.isAdmin()) {
            editable = true;
            deletable = true;
        }

        //회원 -> 직접 작성한 게시글만 삭제, 수정 가능
        Member member = memberUtil.getMember();
        if(_member != null && memberUtil.isLogin() && _member.getUserId().equals(member.getUserId())) {
            editable = true;
            deletable = true;
            mine = true;
        }
        //비회원 -> 비회원 비밀 번호가 확인된 경우 삭제, 수정 가능
        //비회원 비밀 번호 인증 여부 세션에 있는 guest_confirmed_ 게시글 번호 true -> 인증
        HttpSession session = request.getSession();
        String key = "guest_confirmed_" + boardData.getSeq();
        Boolean guestConfirmed = (Boolean) session.getAttribute(key); //비밀번호를 검증

        if(_member == null && guestConfirmed != null && guestConfirmed) {
            editable = true;
            deletable = true;
            mine = true;
        }

        boardData.setEditable(editable);
        boardData.setDeletable(deletable);
        boardData.setMine(mine);

        //수정 버튼 노출 여부
        //관리자 - 노출, 회원 게시글 - 직접 작성한 게시글, 비회원
        boolean showEditButton = memberUtil.isAdmin() || mine || _member == null;
        boolean showDeleteButton = showEditButton;

        boardData.setShowEditButton(showEditButton);
        boardData.setShowDeleteButton(showDeleteButton);

        /* 수정, 삭제 권한 정보 E */

        /* 댓글 작성 권한 처리 S */
        boolean commentable = false;
        Board board = boardData.getBoard();
        Authority commentAccessType = board.getCommentAccessType();
        // 관리자이거나 전체 작성 가능이면
        if (commentAccessType == Authority.ALL) {
            commentable = true;
        }

        if (memberUtil.isLogin()) {
            if (commentAccessType == Authority.USER) {
                commentable = true;
            }

            if (commentAccessType == Authority.ADMIN && memberUtil.isAdmin()) {
                commentable = true;
            }
        }

        boardData.setCommentable(commentable);
        /* 댓글 작성 권한 처리 E */
    }


    /**
     * 게시글 조회수 업데이트
     *
     * @param seq : 게시글 번호
     */
    public void updateViewCount(Long seq) {

        BoardData data = boardDataRepository.findById(seq).orElse(null);
        if (data == null) return;

        try {
            int uid = memberUtil.isLogin() ?
                    memberUtil.getMember().getUserNo().intValue() : utils.guestUid();

            BoardView boardView = new BoardView(seq, uid);

            boardViewRepository.saveAndFlush(boardView);
        } catch (Exception e) {}

        // 조회수 카운팅 -> 게시글에 업데이트
        QBoardView bv = QBoardView.boardView;
        int viewCount = (int)boardViewRepository.count(bv.seq.eq(seq));

        data.setViewCount(viewCount);

        boardViewRepository.flush();

    }
}