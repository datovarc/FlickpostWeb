package co.flickpost.admin.controllers;

import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.WeightProcessResponse;
import co.flickpost.admin.services.InlineWeightProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weight")
public class WeightIngestController {

    @Autowired
    private InlineWeightProcessService inlineWeightProcessService;

    @PostMapping("/incoming")
    public WeightProcessResponse receiveIncomingWeight(@RequestBody SessionPackage packageInfo) {
        return inlineWeightProcessService.processIncoming(packageInfo);
    }
}
