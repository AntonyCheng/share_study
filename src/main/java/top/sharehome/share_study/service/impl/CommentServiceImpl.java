package top.sharehome.share_study.service.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharehome.share_study.common.constant.CommonConstant;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeFileException;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeReturnException;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeTransactionException;
import top.sharehome.share_study.common.response.R;
import top.sharehome.share_study.common.response.RCodeEnum;
import top.sharehome.share_study.mapper.CommentMapper;
import top.sharehome.share_study.mapper.ResourceMapper;
import top.sharehome.share_study.mapper.TeacherMapper;
import top.sharehome.share_study.model.dto.CommentGetDto;
import top.sharehome.share_study.model.dto.CommentPageDto;
import top.sharehome.share_study.model.dto.ResourcePageDto;
import top.sharehome.share_study.model.dto.TeacherLoginDto;
import top.sharehome.share_study.model.entity.Comment;
import top.sharehome.share_study.model.entity.Resource;
import top.sharehome.share_study.model.entity.Teacher;
import top.sharehome.share_study.model.vo.CommentPageVo;
import top.sharehome.share_study.model.vo.CommentUpdateVo;
import top.sharehome.share_study.service.CommentService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论交流ServiceImpl
 *
 * @author AntonyCheng
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @javax.annotation.Resource
    private CommentMapper commentMapper;

    @javax.annotation.Resource
    private TeacherMapper teacherMapper;

    @javax.annotation.Resource
    private ResourceMapper resourceMapper;

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void download(HttpServletResponse response) {
        try {
            // 设置下载信息
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("评论交流数据", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            // 查询课程分类表所有的数据
            List<Comment> resourceList = commentMapper.selectList(null);
            EasyExcelFactory.write(response.getOutputStream(), Comment.class)
                    .sheet("评论交流数据")
                    .doWrite(resourceList);
        } catch (UnsupportedEncodingException e) {
            throw new CustomizeFileException(R.failure(RCodeEnum.EXCEL_EXPORT_FAILED), "导出Excel时文件编码异常");
        } catch (IOException e) {
            throw new CustomizeFileException(R.failure(RCodeEnum.EXCEL_EXPORT_FAILED), "文件写入时，响应流发生异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void delete(Long id, HttpServletRequest request) {
        TeacherLoginDto teacherLoginDto = (TeacherLoginDto) request.getSession().getAttribute(CommonConstant.ADMIN_LOGIN_STATE);
        if (Objects.equals(teacherLoginDto.getRole(), CommonConstant.DEFAULT_ROLE)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "非管理员不能进行删除操作");
        }

        LambdaQueryWrapper<Comment> resourceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        resourceLambdaQueryWrapper.eq(Comment::getId, id);

        Comment selectResult = commentMapper.selectOne(resourceLambdaQueryWrapper);
        if (selectResult == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COMMENT_NOT_EXISTS), "交流评论不存在，不需要进行下一步操作");
        }

        Teacher targetTeacher = teacherMapper.selectById(selectResult.getBelong());
        if (targetTeacher == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "发表该交流评论的老师不存在");
        }

        Resource targetResource = resourceMapper.selectById(selectResult.getResource());
        if (targetResource == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "该交流评论所在的教学资料不存在");
        }

        if (!Objects.equals(targetTeacher.getId(), teacherLoginDto.getId())
                && (Objects.equals(teacherLoginDto.getRole(), CommonConstant.ADMIN_ROLE)
                && !Objects.equals(targetTeacher.getRole(), CommonConstant.DEFAULT_ROLE))) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "管理员没有权限在此删除其他管理员和超级管理员的交流评论");
        }

        int deleteResult = commentMapper.delete(resourceLambdaQueryWrapper);

        if (deleteResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_DELETION_FAILED), "交流评论数据删除失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void deleteBatch(List<Long> ids, HttpServletRequest request) {
        TeacherLoginDto teacherLoginDto = (TeacherLoginDto) request.getSession().getAttribute(CommonConstant.ADMIN_LOGIN_STATE);
        if (Objects.equals(teacherLoginDto.getRole(), CommonConstant.DEFAULT_ROLE)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "非管理员不能进行删除操作");
        }

        ids.forEach(id -> {
            Comment selectResult = commentMapper.selectById(id);
            if (selectResult == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "交流评论不存在，不需要进行下一步操作");
            }

            Teacher targetTeacher = teacherMapper.selectById(selectResult.getBelong());
            if (targetTeacher == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "发表该交流评论的老师不存在");
            }

            Resource targetResource = resourceMapper.selectById(selectResult.getResource());
            if (targetResource == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "该交流评论所在的教学资料不存在");
            }

            if (!Objects.equals(targetTeacher.getId(), teacherLoginDto.getId())
                    && (Objects.equals(teacherLoginDto.getRole(), CommonConstant.ADMIN_ROLE)
                    && !Objects.equals(targetTeacher.getRole(), CommonConstant.DEFAULT_ROLE))) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "管理员没有权限在此删除其他管理员和超级管理员的交流评论");
            }
        });

        int deleteResult = resourceMapper.deleteBatchIds(ids);
        if (deleteResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_DELETION_FAILED), "交流评论数据删除失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public CommentGetDto get(Long id, HttpServletRequest request) {
        TeacherLoginDto teacherLoginDto = (TeacherLoginDto) request.getSession().getAttribute(CommonConstant.ADMIN_LOGIN_STATE);
        if (Objects.equals(teacherLoginDto.getRole(), CommonConstant.DEFAULT_ROLE)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "非管理员不能进行删除操作");
        }

        LambdaQueryWrapper<Comment> resourceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        resourceLambdaQueryWrapper.eq(Comment::getId, id);

        Comment selectResult = commentMapper.selectOne(resourceLambdaQueryWrapper);
        if (selectResult == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "评论交流不存在，不需要进行下一步操作");
        }

        Teacher targetTeacher = teacherMapper.selectById(selectResult.getBelong());
        if (targetTeacher == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "发表该教学资料的老师不存在");
        }

        Resource targetResource = resourceMapper.selectById(selectResult.getResource());
        if (targetResource == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "该评论交流所属教学资料不存在");
        }

        if (!Objects.equals(targetTeacher.getId(), teacherLoginDto.getId())
                && (Objects.equals(teacherLoginDto.getRole(), CommonConstant.ADMIN_ROLE)
                && !Objects.equals(targetTeacher.getRole(), CommonConstant.DEFAULT_ROLE))) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "管理员没有权限在此回显其他管理员和超级管理员的教学资料");
        }

        CommentGetDto commentGetDto = new CommentGetDto();
        commentGetDto.setId(selectResult.getId());
        commentGetDto.setStatus(selectResult.getStatus());

        return commentGetDto;
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void updateComment(CommentUpdateVo commentUpdateVo, HttpServletRequest request) {
        TeacherLoginDto teacherLoginDto = (TeacherLoginDto) request.getSession().getAttribute(CommonConstant.ADMIN_LOGIN_STATE);
        if (Objects.equals(teacherLoginDto.getRole(), CommonConstant.DEFAULT_ROLE)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "非管理员不能进行删除操作");
        }

        Comment resultFromDatabase = commentMapper.selectById(commentUpdateVo.getId());
        if (resultFromDatabase == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "交流评论不存在，不需要进行下一步操作");
        }
        if (Objects.equals(commentUpdateVo.getStatus(), resultFromDatabase.getStatus())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.THE_UPDATE_DATA_IS_THE_SAME_AS_THE_BACKGROUND_DATA), "更新数据和库中数据相同");
        }

        Teacher targetTeacher = teacherMapper.selectById(resultFromDatabase.getBelong());
        if (targetTeacher == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "发表该交流评论的老师不存在");
        }

        Resource targetResource = resourceMapper.selectById(resultFromDatabase.getResource());
        if (targetResource == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "交流评论所在的教学资料不存在");
        }

        if (!Objects.equals(targetTeacher.getId(), teacherLoginDto.getId())
                && (Objects.equals(teacherLoginDto.getRole(), CommonConstant.ADMIN_ROLE)
                && !Objects.equals(targetTeacher.getRole(), CommonConstant.DEFAULT_ROLE))) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.ACCESS_UNAUTHORIZED), "管理员没有权限在此修改其他管理员和超级管理员的交流评论");
        }

        resultFromDatabase.setStatus(commentUpdateVo.getStatus());

        int updateResult = commentMapper.updateById(resultFromDatabase);

        // 判断数据库插入结果
        if (updateResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_MODIFICATION_FAILED), "修改交流评论失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    // TODO:完善评论分页功能
    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public Page<CommentPageDto> pageComment(Integer current, Integer pageSize, CommentPageVo commentPageVo) {
        Page<Comment> page = new Page<>(current, pageSize);
        Page<CommentPageDto> returnResult = new Page<>(current, pageSize);

        if (commentPageVo == null) {
            this.page(page);
            BeanUtils.copyProperties(page, returnResult, "records");
            List<CommentPageDto> pageDtoList = page.getRecords().stream().map(record -> {
                CommentPageDto commentPageDto = new CommentPageDto();
                BeanUtils.copyProperties(record, commentPageDto);

                LambdaQueryWrapper<Teacher> belongLambdaQueryWrapper = new LambdaQueryWrapper<>();
                belongLambdaQueryWrapper.eq(Teacher::getId, record.getBelong());
                Teacher belong = teacherMapper.selectOne(belongLambdaQueryWrapper);
                if (belong == null) {
                    throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "发送者不存在");
                }
                commentPageDto.setBelongName(belong.getName());

                LambdaQueryWrapper<Teacher> sendLambdaQueryWrapper = new LambdaQueryWrapper<>();
                sendLambdaQueryWrapper.eq(Teacher::getId, record.getSend());
                Teacher send = teacherMapper.selectOne(sendLambdaQueryWrapper);
                if (send == null) {
                    throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS), "接收者不存在");
                }
                commentPageDto.setSendName(send.getName());

                LambdaQueryWrapper<Resource> resourceQueryWrapper = new LambdaQueryWrapper<>();
                resourceQueryWrapper.eq(Resource::getId, record.getResource());
                Resource resource = resourceMapper.selectOne(resourceQueryWrapper);
                if (resource == null) {
                    throw new CustomizeReturnException(R.failure(RCodeEnum.RESOURCE_NOT_EXISTS), "教学资料不存在");
                }
                commentPageDto.setResourceName(resource.getName());

                return commentPageDto;
            }).collect(Collectors.toList());
            returnResult.setRecords(pageDtoList);
            return returnResult;
        }

        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .like(!StringUtils.isEmpty(commentPageVo.getContent()), Comment::getContent, commentPageVo.getContent())
                .like(!ObjectUtils.isEmpty(commentPageVo.getReadStatus()), Comment::getReadStatus, commentPageVo.getReadStatus())
                .like(!ObjectUtils.isEmpty(commentPageVo.getStatus()), Comment::getStatus, commentPageVo.getStatus())
                .orderByAsc(Comment::getCreateTime);
        this.page(page, lambdaQueryWrapper);
        BeanUtils.copyProperties(page, returnResult, "records");

        String belongName = commentPageVo.getBelongName();
        List<Long> belongIds = new ArrayList<>();
        if (!StringUtils.isEmpty(belongName)) {
            LambdaQueryWrapper<Teacher> belongNameLambdaQueryWrapper = new LambdaQueryWrapper<>();
            belongNameLambdaQueryWrapper.like(Teacher::getName, belongName);
            List<Teacher> teachers = teacherMapper.selectList(belongNameLambdaQueryWrapper);
            belongIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());
        }
        String sendName = commentPageVo.getBelongName();
        List<Long> sendIds = new ArrayList<>();
        if (!StringUtils.isEmpty(sendName)) {
            LambdaQueryWrapper<Teacher> sendNameLambdaQueryWrapper = new LambdaQueryWrapper<>();
            sendNameLambdaQueryWrapper.like(Teacher::getName, sendName);
            List<Teacher> teachers = teacherMapper.selectList(sendNameLambdaQueryWrapper);
            sendIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());
        }
        String resourceName = commentPageVo.getResourceName();
        List<Long> resourceIds = new ArrayList<>();
        if (!StringUtils.isEmpty(resourceName)) {
            LambdaQueryWrapper<Resource> resourceNameLambdaQueryWrapper = new LambdaQueryWrapper<>();
            resourceNameLambdaQueryWrapper.like(Resource::getName, resourceName);
            List<Resource> teachers = resourceMapper.selectList(resourceNameLambdaQueryWrapper);
            resourceIds = teachers.stream().map(Resource::getId).collect(Collectors.toList());
        }

        List<Long> finalBelongIds = belongIds;
        List<Long> finalSendIds = sendIds;
        List<Long> finalResourceIds = resourceIds;
        if (finalBelongIds.isEmpty()) {
            page.setRecords(new ArrayList<>());
        }
        if (finalSendIds.isEmpty()) {
            page.setRecords(new ArrayList<>());
        }
        if (finalResourceIds.isEmpty()) {
            page.setRecords(new ArrayList<>());
        }

        List<CommentPageDto> pageDtoList = page.getRecords().stream().map(record -> {
            if (!finalBelongIds.isEmpty() && !finalBelongIds.contains(record.getBelong())) {
                return null;
            }
            if (!finalSendIds.isEmpty() && !finalSendIds.contains(record.getSend())) {
                return null;
            }
            if (!finalResourceIds.isEmpty() && !finalResourceIds.contains(record.getResource())) {
                return null;
            }

            CommentPageDto commentPageDto = new CommentPageDto();
            BeanUtils.copyProperties(record, commentPageDto);

            LambdaQueryWrapper<Teacher> belongLambdaQueryWrapper = new LambdaQueryWrapper<>();
            belongLambdaQueryWrapper.eq(Teacher::getId, record.getBelong());
            Teacher belongTeacher = teacherMapper.selectOne(belongLambdaQueryWrapper);
            if (belongTeacher == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS));
            }
            commentPageDto.setBelongName(belongTeacher.getName());

            LambdaQueryWrapper<Teacher> sendLambdaQueryWrapper = new LambdaQueryWrapper<>();
            sendLambdaQueryWrapper.eq(Teacher::getId, record.getSend());
            Teacher sendTeacher = teacherMapper.selectOne(sendLambdaQueryWrapper);
            if (sendTeacher == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS));
            }
            commentPageDto.setSendName(sendTeacher.getName());

            LambdaQueryWrapper<Resource> resourceLambdaQueryWrapper = new LambdaQueryWrapper<>();
            resourceLambdaQueryWrapper.eq(Resource::getId, record.getResource());
            Resource resource = resourceMapper.selectOne(resourceLambdaQueryWrapper);
            if (resource == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.TEACHER_NOT_EXISTS));
            }
            commentPageDto.setResourceName(resource.getName());

            return commentPageDto;
        }).collect(Collectors.toList());
        returnResult.setRecords(pageDtoList);
        return returnResult;
    }
}
