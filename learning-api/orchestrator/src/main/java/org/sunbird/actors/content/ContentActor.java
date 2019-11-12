package org.sunbird.actors.content;

import akka.dispatch.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.ContentParams;
import org.sunbird.common.dto.Request;
import org.sunbird.common.dto.Response;
import org.sunbird.common.dto.ResponseHandler;
import org.sunbird.common.exception.ClientException;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.graph.nodes.DataNode;
import scala.concurrent.Future;


public class ContentActor extends BaseActor {

    public Future<Response> onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        if ("createContent".equals(operation)) {
            return create(request);
        } else {
            return ERROR(operation);
        }
    }

    private Future<Response> create(Request request) throws Exception {
        populateDefaultersForCreation(request);
        return DataNode.create(request, getContext().dispatcher())
                .map(new Mapper<Node, Response>() {
                    @Override
                    public Response apply(Node node) {
                        Response response = ResponseHandler.OK();
                        response.put("node_id", node.getIdentifier());
                        response.put("versionKey", node.getMetadata().get("versionKey"));
                        return response;
                    }
                }, getContext().dispatcher());
    }

    private void update(Request request) {
    }

    private static void populateDefaultersForCreation(Request request) {
        setDefaultsBasedOnMimeType(request, ContentParams.create.name());
        validateLicense(request);
    }

    private static void validateLicense(Request request) {
        //TODO: for license validation
    }

    private static void setDefaultsBasedOnMimeType(Request request, String operation) {

        String mimeType = (String) request.get(ContentParams.mimeType.name());
        if (StringUtils.isNotBlank(mimeType) && operation.equalsIgnoreCase(ContentParams.create.name())) {
            if(StringUtils.equalsIgnoreCase("application/vnd.ekstep.plugin-archive", mimeType)) {
                String code = (String) request.get(ContentParams.code.name());
                if (null == code || StringUtils.isBlank(code))
                    throw new ClientException("ERR_PLUGIN_CODE_REQUIRED", "Unique code is mandatory for plugins");
                request.put(ContentParams.identifier.name(), request.get(ContentParams.code.name()));
            } else {
                request.put(ContentParams.osId.name(), "org.ekstep.quiz.app");
            }

            if (mimeType.endsWith("archive") || mimeType.endsWith("vnd.ekstep.content-collection")
                    || mimeType.endsWith("epub"))
                request.put(ContentParams.contentEncoding.name(), ContentParams.gzip.name());
            else
                request.put(ContentParams.contentEncoding.name(), ContentParams.identity.name());

            if (mimeType.endsWith("youtube") || mimeType.endsWith("x-url"))
                request.put(ContentParams.contentDisposition.name(), ContentParams.online.name());
            else
                request.put(ContentParams.contentDisposition.name(), ContentParams.inline.name());
        }
    }


}