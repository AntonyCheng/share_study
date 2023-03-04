package top.sharehome.share_study.controller;

import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.sharehome.share_study.common.exception_handler.customize.CustomizeReturnException;
import top.sharehome.share_study.common.response.R;
import top.sharehome.share_study.common.response.RCodeEnum;
import top.sharehome.share_study.model.dto.TeacherLoginDto;
import top.sharehome.share_study.model.dto.UserGetInfoDto;
import top.sharehome.share_study.model.dto.UserResourceGetDto;
import top.sharehome.share_study.model.dto.UserResourcePageDto;
import top.sharehome.share_study.model.vo.UserResourcePageVo;
import top.sharehome.share_study.model.vo.UserResourceUpdateVo;
import top.sharehome.share_study.model.vo.UserUpdateInfoVo;
import top.sharehome.share_study.service.ResourceService;
import top.sharehome.share_study.service.TeacherService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 普通用户相关接口
 *
 * @author AntonyCheng
 */
@RestController
@RequestMapping("/user")
@Api(tags = "普通用户相关接口")
@CrossOrigin
public class UserController {
    @Resource
    private TeacherService teacherService;

    @Resource
    private ResourceService resourceService;

    /**
     * 账号的匹配表达式
     */
    private static final String MATCHER_ACCOUNT_REGEX = "^[\u4E00-\u9FA5A-Za-z0-9]{4,16}$";
    /**
     * 姓名的匹配表达式
     */
    private static final String MATCHER_NAME_REGEX = "^[\u4e00-\u9fa5.·]{0,}$";
    /**
     * 邮箱的匹配表达式
     */
    private static final String MATCHER_EMAIL_REGEX = "([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}";
    /**
     * 账号长度最小值
     */
    private static final Integer ACCOUNT_GE_LENGTH = 4;
    /**
     * 账号长度最大值
     */
    private static final Integer ACCOUNT_LE_LENGTH = 16;

    /**
     * 普通用户获取登录状态（需要有登录状态才能获取）
     *
     * @param id      前端传来的操作者的id
     * @param request 获取Session中的登录状态
     * @return 返回最新的登录状态
     */
    @GetMapping("/login/get/{id}")
    @ApiOperation("普通用户获取登录状态")
    public R<TeacherLoginDto> getUserLogin(@PathVariable("id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY), "操作者id为空，操作失败");
        }
        TeacherLoginDto teacherLoginDto = teacherService.getUserLogin(id, request);
        return R.success(teacherLoginDto, "状态存在，更新状态成功");
    }

    /**
     * 普通用户的教学资料分页
     *
     * @param id       普通用户的ID
     * @param current  当前页
     * @param pageSize 页面条数
     * @param request  获取操作者的登录状态
     * @return 返回分页结果
     */
    @GetMapping("/resource/page/{current}/{pageSize}/{id}")
    @ApiOperation("普通用户的教学资料分页")
    public R<Page<UserResourcePageDto>> getResourcePage(@PathVariable("id") Long id, @PathVariable("current") Integer current, @PathVariable("pageSize") Integer pageSize, HttpServletRequest request, @RequestBody(required = false) UserResourcePageVo userResourcePageVo) {
        if (id == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY), "操作者id为空，操作失败");
        }

        // 判空
        if (ObjectUtils.isEmpty(current) || ObjectUtils.isEmpty(pageSize)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY), "分页参数为空");
        }

        // 判断数据格式
        if (current <= 0 || pageSize <= 0) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.PARAMETER_FORMAT_MISMATCH), "分页参数格式错误");
        }

        Page<UserResourcePageDto> userResourceDtoPage = resourceService.getUserResourcePage(id, current, pageSize, request, userResourcePageVo);

        return R.success(userResourceDtoPage, "用户教学资料查询成功");
    }

    /**
     * 普通用户删除教学资料
     *
     * @param id      教学资料的ID
     * @param request 获取操作者的登录状态
     * @return 返回分页结果
     */
    @GetMapping("/resource/delete/{id}")
    @ApiOperation("普通用户删除教学资料")
    public R<String> deleteResource(@PathVariable("id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY), "操作者id为空，操作失败");
        }
        resourceService.deleteUserResource(id, request);
        return R.success("删除成功");
    }

    /**
     * 普通用户获取自己信息接口（s/a/u）
     *
     * @param id      普通用户ID
     * @param request 获取Session中登录状态
     * @return 返回普通用户自己的可修改信息
     */
    @GetMapping("/info/get/{id}")
    @ApiOperation("普通用户获取自己信息接口")
    public R<UserGetInfoDto> getInfo(@PathVariable("id") Long id, HttpServletRequest request) {
        // 判空
        if (ObjectUtils.isEmpty(id)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY));
        }

        // 执行获取自身信息的操作
        UserGetInfoDto userGetInfoDto = teacherService.getUserSelf(id, request);

        return R.success(userGetInfoDto, "回显成功");
    }

    /**
     * 普通用户修改自己信息接口（s/a/u）
     *
     * @param userUpdateInfoVo 普通用户更新自己信息Vo实体
     * @param request          获取Session中的登录状态
     * @return 返回更新结果
     */
    @PutMapping("/info/update")
    @ApiOperation("普通用户修改自己信息接口")
    public R<String> updateInfo(@RequestBody UserUpdateInfoVo userUpdateInfoVo, HttpServletRequest request) {
        // 判空
        if (userUpdateInfoVo == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY));
        }
        if (StringUtils.isAnyEmpty(
                userUpdateInfoVo.getAccount(),
                userUpdateInfoVo.getPassword(),
                userUpdateInfoVo.getName())
                || ObjectUtils.isEmpty(userUpdateInfoVo.getId())
                || ObjectUtils.isEmpty(userUpdateInfoVo.getGender())
                || ObjectUtils.isEmpty(userUpdateInfoVo.getBelong())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY));
        }

        // 判断性别数据是否正确
        if (!(userUpdateInfoVo.getGender() == 0 || userUpdateInfoVo.getGender() == 1)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.PARAMETER_FORMAT_MISMATCH));
        }

        // 校验账户长度
        if (userUpdateInfoVo.getAccount().length() > ACCOUNT_LE_LENGTH || userUpdateInfoVo.getAccount().length() < ACCOUNT_GE_LENGTH) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.USERNAME_LENGTH_DO_NOT_MATCH), "用户账户的长度不匹配");
        }

        // 校验账户格式
        if (!ReUtil.isMatch(MATCHER_ACCOUNT_REGEX, userUpdateInfoVo.getAccount())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.USERNAME_CONTAINS_SPECIAL_CHARACTERS), "用户账户中包含特殊字符");
        }

        // 校验姓名格式
        if (!ReUtil.isMatch(MATCHER_NAME_REGEX, userUpdateInfoVo.getName())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.NAME_FORMAT_VERIFICATION_FAILED), "姓名格式有误");
        }

        // 校验邮箱格式
        if (!ObjectUtils.isEmpty(userUpdateInfoVo.getEmail()) && !ReUtil.isMatch(MATCHER_EMAIL_REGEX, userUpdateInfoVo.getEmail())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.EMAIL_FORMAT_VERIFICATION_FAILED), "邮箱格式有误");
        }

        // 执行更新自身信息的操作
        teacherService.updateUserSelf(userUpdateInfoVo, request);

        return R.success("修改成功");
    }

    /**
     * 普通用户获取教学资料信息接口（s/a/u）
     *
     * @param id      教学资料ID
     * @param request 获取登录的Session状态
     * @return 返回教学资料可修改信息
     */
    @GetMapping("/resource/get/{id}")
    @ApiOperation("普通用户获取教学资料信息接口")
    public R<UserResourceGetDto> get(@PathVariable("id") Long id, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(id)) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY), "教学资料ID为空");
        }
        UserResourceGetDto userResourceGetDto = resourceService.getUserResource(id, request);

        return R.success(userResourceGetDto, "回显成功");
    }

    /**
     * 普通用户修改教学资料信息接口（s/a/u）
     *
     * @param userResourceUpdateVo 普通用户更新教学资料信息Vo实体
     * @return 返回更新结果
     */
    @PutMapping("/resource/update")
    @ApiOperation("普通用户修改教学资料信息接口")
    public R<String> update(@RequestBody UserResourceUpdateVo userResourceUpdateVo, HttpServletRequest request) {
        if (userResourceUpdateVo == null) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY));
        }

        if (ObjectUtils.isEmpty(userResourceUpdateVo.getId())
                || StringUtils.isEmpty(userResourceUpdateVo.getInfo())
                || StringUtils.isEmpty(userResourceUpdateVo.getUrl())
                || StringUtils.isEmpty(userResourceUpdateVo.getName())) {
            throw new CustomizeReturnException(R.failure(RCodeEnum.REQUEST_REQUIRED_PARAMETER_IS_EMPTY));
        }

        resourceService.updateUserResource(userResourceUpdateVo, request);

        return R.success("修改成功");
    }
}