package com.lablink.cloudmind.module.integration.lablink.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.integration.lablink.application.LabLinkFileSyncApplicationService;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkFileDeleteSyncRequest;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkFileSyncRequest;
import com.lablink.cloudmind.module.integration.lablink.security.LabLinkInternalAuthService;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkFileDeleteSyncVO;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkFileSyncVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * LabLink 内部文件同步接口。
 */
@RestController
@RequestMapping("/api/internal/lablink/files")
@RequiredArgsConstructor
public class LabLinkInternalFileController {

    private final LabLinkInternalAuthService labLinkInternalAuthService;

    private final LabLinkFileSyncApplicationService labLinkFileSyncApplicationService;

    @PostMapping("/sync")
    public Result<LabLinkFileSyncVO> syncFile(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody LabLinkFileSyncRequest request
    ) {
        labLinkInternalAuthService.verifyInternalToken(internalToken);

        return Result.success(
                labLinkFileSyncApplicationService.syncFile(request)
        );
    }

    @PostMapping("/delete-sync")
    public Result<LabLinkFileDeleteSyncVO> deleteSync(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody LabLinkFileDeleteSyncRequest request
    ) {
        labLinkInternalAuthService.verifyInternalToken(internalToken);

        return Result.success(
                labLinkFileSyncApplicationService.deleteSync(request)
        );
    }

}