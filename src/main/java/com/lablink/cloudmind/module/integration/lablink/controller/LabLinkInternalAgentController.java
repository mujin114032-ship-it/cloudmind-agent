package com.lablink.cloudmind.module.integration.lablink.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.integration.lablink.application.LabLinkAgentApplicationService;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkAgentBootstrapRequest;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkSaveLlmKeyRequest;
import com.lablink.cloudmind.module.integration.lablink.security.LabLinkInternalAuthService;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkAgentBootstrapVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * LabLink 内部 Agent 接口。
 *
 * <p>仅允许 LabLink 后端通过 internal-token 调用，不直接暴露给浏览器。</p>
 */
@RestController
@RequestMapping("/api/internal/lablink/agent")
@RequiredArgsConstructor
public class LabLinkInternalAgentController {

    private final LabLinkInternalAuthService labLinkInternalAuthService;

    private final LabLinkAgentApplicationService labLinkAgentApplicationService;

    @PostMapping("/bootstrap")
    public Result<LabLinkAgentBootstrapVO> bootstrap(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody LabLinkAgentBootstrapRequest request
    ) {
        labLinkInternalAuthService.verifyInternalToken(internalToken);

        return Result.success(
                labLinkAgentApplicationService.bootstrap(request)
        );
    }

    @PostMapping("/llm-key")
    public Result<LabLinkAgentBootstrapVO> saveLlmKey(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody LabLinkSaveLlmKeyRequest request
    ) {
        labLinkInternalAuthService.verifyInternalToken(internalToken);

        return Result.success(
                labLinkAgentApplicationService.saveLlmKey(request)
        );
    }

}