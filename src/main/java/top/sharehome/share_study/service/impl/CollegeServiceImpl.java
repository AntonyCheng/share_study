package top.sharehome.share_study.service.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeFileException;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeReturnException;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeTransactionException;
import top.sharehome.share_study.common.response.R;
import top.sharehome.share_study.common.response.RCodeEnum;
import top.sharehome.share_study.mapper.CollegeMapper;
import top.sharehome.share_study.mapper.TeacherMapper;
import top.sharehome.share_study.model.dto.CollegeGetDto;
import top.sharehome.share_study.model.dto.CollegePageDto;
import top.sharehome.share_study.model.entity.College;
import top.sharehome.share_study.model.entity.Teacher;
import top.sharehome.share_study.model.vo.CollegeAddVo;
import top.sharehome.share_study.model.vo.CollegePageVo;
import top.sharehome.share_study.model.vo.CollegeUpdateVo;
import top.sharehome.share_study.service.CollegeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高校ServiceImpl
 *
 * @author AntonyCheng
 */
@Service
public class CollegeServiceImpl extends ServiceImpl<CollegeMapper, College> implements CollegeService {
    @Resource
    private CollegeMapper collegeMapper;

    @Resource
    private TeacherMapper teacherMapper;

    @Override
    @Transactional(rollbackFor = CustomizeReturnException.class)
    public void add(CollegeAddVo collegeAddVo) {
        // 判断数据库中数据是否重复
        LambdaQueryWrapper<College> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(College::getName, collegeAddVo.getName())
                .or()
                .eq(College::getCode, collegeAddVo.getCode());
        Long resultFromDatabase = collegeMapper.selectCount(queryWrapper);
        if (resultFromDatabase != 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_ALREADY_EXISTS), "数据库中已经包含该高校：" + collegeAddVo.getName() + "|" + collegeAddVo.hashCode());
        }

        // 进行数据拷贝和插入
        College college = new College();
        BeanUtils.copyProperties(collegeAddVo, college);

        int insertResult = collegeMapper.insert(college);

        // 判断数据库插入结果
        if (insertResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_ADDITION_FAILED), "添加高校失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeReturnException.class)
    public void delete(Long id) {
        // 判断高校是否存在
        LambdaQueryWrapper<College> collegeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        collegeLambdaQueryWrapper.eq(College::getId, id);
        College selectResult = collegeMapper.selectOne(collegeLambdaQueryWrapper);
        if (selectResult == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_NOT_EXISTS), "高校不存在，不需要进行下一步操作");
        }

        // 判断该高校是否还绑定着用户
        Long collegeId = selectResult.getId();
        LambdaQueryWrapper<Teacher> teacherLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teacherLambdaQueryWrapper.eq(Teacher::getBelong, collegeId);
        Long teacherCount = teacherMapper.selectCount(teacherLambdaQueryWrapper);
        if (teacherCount != 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_BIND_USER), "高校下还绑定着用户，无法删除");
        }

        LambdaUpdateWrapper<College> collegeLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        collegeLambdaUpdateWrapper.eq(College::getId, id);
        selectResult.setName(selectResult.getName() + "+" + System.currentTimeMillis());
        selectResult.setCode(selectResult.getCode() + "+" + System.currentTimeMillis());
        collegeMapper.update(selectResult, collegeLambdaUpdateWrapper);

        int deleteResult = collegeMapper.delete(collegeLambdaQueryWrapper);

        // 判断数据库删除结果
        if (deleteResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_DELETION_FAILED), "高校数据删除失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public CollegeGetDto get(Long id) {
        // 判断高校是否存在
        LambdaQueryWrapper<College> collegeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        collegeLambdaQueryWrapper.eq(College::getId, id);
        College selectResult = collegeMapper.selectOne(collegeLambdaQueryWrapper);
        if (selectResult == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_NOT_EXISTS), "高校不存在，不需要进行下一步操作");
        }

        // 信息脱敏
        CollegeGetDto collegeGetDto = new CollegeGetDto();
        collegeGetDto.setId(selectResult.getId());
        collegeGetDto.setName(selectResult.getName());
        collegeGetDto.setCode(selectResult.getCode());

        return collegeGetDto;
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void updateCollege(CollegeUpdateVo collegeUpdateVo) {
        // 判断高校是否存在
        College selectResult = collegeMapper.selectById(collegeUpdateVo.getId());
        if (selectResult == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_NOT_EXISTS), "修改的高校对象并不在数据库中");
        }

        // 判断数据前后是否发生变化
        if (selectResult.getCode().equals(collegeUpdateVo.getCode()) && selectResult.getName().equals(collegeUpdateVo.getName())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_HAS_NOT_CHANGED_BEFORE_THE_MODIFICATION), "数据修改前后未发生变化");
        }

        // 判断更新的院校是否重复
        LambdaQueryWrapper<College> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(College::getName, collegeUpdateVo.getName())
                .or()
                .eq(College::getCode, collegeUpdateVo.getCode());
        College checkCollege = collegeMapper.selectOne(queryWrapper);
        if (checkCollege != null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DUPLICATION_OF_INSTITUTIONS), "院校重复");
        }

        // 包装新的高校对象
        College college = new College();
        college.setId(collegeUpdateVo.getId());
        college.setName(collegeUpdateVo.getName());
        college.setCode(collegeUpdateVo.getCode());

        int updateResult = collegeMapper.updateById(college);

        // 判断数据库更新结果
        if (updateResult == 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.DATA_MODIFICATION_FAILED), "高校数据修改失败，从数据库返回的影响行数为0，且在之前没有报出异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public Page<CollegePageDto> pageCollege(Integer current, Integer pageSize, CollegePageVo collegePageVo) {
        // 创建原始分页数据以及返回分页数据
        Page<College> page = new Page<>(current, pageSize);
        Page<CollegePageDto> returnResult = new Page<>(current, pageSize);

        // 过滤分页对象
        LambdaQueryWrapper<College> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .orderByAsc(College::getCreateTime);

        // 当不存在模糊查询时的分页操作
        if (collegePageVo == null) {
            this.page(page);
            BeanUtils.copyProperties(page, returnResult, "records");
            List<CollegePageDto> pageDtoList = page.getRecords().stream().map(record -> {
                CollegePageDto collegePageDto = new CollegePageDto();
                BeanUtils.copyProperties(record, collegePageDto);
                return collegePageDto;
            }).collect(Collectors.toList());
            returnResult.setRecords(pageDtoList);
            return returnResult;
        }

        // 当存在模糊查询时的分页操作
        lambdaQueryWrapper
                .like(!StringUtils.isEmpty(collegePageVo.getName()), College::getName, collegePageVo.getName())
                .like(!StringUtils.isEmpty(collegePageVo.getCode()), College::getCode, collegePageVo.getCode());
        this.page(page, lambdaQueryWrapper);
        BeanUtils.copyProperties(page, returnResult, "records");
        List<CollegePageDto> pageDtoList = page.getRecords().stream().map(record -> {
            CollegePageDto collegePageDto = new CollegePageDto();
            BeanUtils.copyProperties(record, collegePageDto);
            return collegePageDto;
        }).collect(Collectors.toList());
        returnResult.setRecords(pageDtoList);
        return returnResult;
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void deleteBath(List<Long> ids) {
        // 判断高校ID是否有对应的高校数据
        ids.forEach(id -> {
            College selectResult = this.getById(id);
            if (selectResult == null) {
                throw new CustomizeReturnException(R.failure(RCodeEnum.COLLEGE_NOT_EXISTS), "所要删除的高校ID中有不存在的高校");
            }
            LambdaUpdateWrapper<College> collegeLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            collegeLambdaUpdateWrapper.eq(College::getId, id);
            selectResult.setName(selectResult.getName() + "+" + System.currentTimeMillis());
            selectResult.setCode(selectResult.getCode() + "+" + System.currentTimeMillis());
            collegeMapper.update(selectResult, collegeLambdaUpdateWrapper);
        });
        this.removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public void download(HttpServletResponse response) {
        try {
            // 设置下载信息
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("高校信息", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            // 查询高校分类表所有的数据
            List<College> collegeList = collegeMapper.selectList(null);
            EasyExcelFactory.write(response.getOutputStream(), College.class)
                    .sheet("高校数据")
                    .doWrite(collegeList);
        } catch (UnsupportedEncodingException e) {
            throw new CustomizeFileException(R.failure(RCodeEnum.EXCEL_EXPORT_FAILED), "导出Excel时文件编码异常");
        } catch (IOException e) {
            throw new CustomizeFileException(R.failure(RCodeEnum.EXCEL_EXPORT_FAILED), "文件写入时，响应流发生异常");
        }
    }

    @Override
    @Transactional(rollbackFor = CustomizeTransactionException.class)
    public List<CollegeGetDto> listCollege() {
        // 获取高校的信息列表
        List<College> list = this.list();
        return list.stream().map(college -> {
            CollegeGetDto collegeGetDto = new CollegeGetDto();
            BeanUtils.copyProperties(college, collegeGetDto);
            return collegeGetDto;
        }).collect(Collectors.toList());
    }
}
