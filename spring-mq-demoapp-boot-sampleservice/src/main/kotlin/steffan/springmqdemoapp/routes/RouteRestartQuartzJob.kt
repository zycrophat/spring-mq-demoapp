package steffan.springmqdemoapp.routes

import org.apache.camel.CamelContext
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean

class RouteRestartQuartzJob(
        @Autowired
        private val camelCtx: CamelContext
): QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val routeId = context.jobDetail.jobDataMap["routeId"] as String
        camelCtx.startRoute(routeId)
    }
}