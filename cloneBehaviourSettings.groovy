import org.codehaus.jackson.map.ObjectMapper
import com.atlassian.jira.project.ProjectManager
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import com.onresolve.jira.behaviours.BehaviourStoragePropertiesImpl
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.jira.component.ComponentAccessor
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


@BaseScript CustomEndpointDelegate delegate

cloneProjectMappingBehaviours(
        httpMethod: "GET", groups: ["jira-administrators"]
) { MultivaluedMap queryParams, String body ->
    int cod;
    int msn;
    try {
        String originProjectKey = queryParams.getFirst("originProjectKey").toString().replace(" ","").toUpperCase()
        String destinationProjectKey = queryParams.getFirst("destinationProjectKey").toString().replace(" ","").toUpperCase()
        cloneProjectMappingBehaviours(originProjectKey,destinationProjectKey)
        cod = 200
        msn = ""
    } catch (Exception e) {
        cod = 500
        msn = e.getMessage()
    } finally {
        return Response.ok(msn).status(cod).build()
    }

}


private void cloneProjectMappingBehaviours(String originProjectKey, String destinationProjectKey) {

    destinationProjectKey = destinationProjectKey.toUpperCase().replace(" ", "")
    originProjectKey = originProjectKey.toUpperCase().replace(" ", "")

    String projectBaseKey = originProjectKey
    String projectToCloneMapKey = destinationProjectKey


    BehaviourStoragePropertiesImpl behavioursStorage = new BehaviourStoragePropertiesImpl()
    Document docMapProjectBehaviours = convertStringToXMLDocument(behavioursStorage.getProjectMappingsAsText().toString())
    ProjectManager projectManager = ComponentAccessor.getProjectManager();
    long projectBaseId = projectManager.getProjectByCurrentKeyIgnoreCase(projectBaseKey).getId()
    long projectToCloneMapId = projectManager.getProjectByCurrentKeyIgnoreCase(projectToCloneMapKey).getId()


    NodeList listProjects = docMapProjectBehaviours.getElementsByTagName("project");
    for (int i = 0; i < listProjects.getLength(); i++) {
        Node nodo = (Node) listProjects.item(i)
        if (nodo.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) nodo
            if (Long.parseLong(e.getAttribute("pid")) == projectBaseId) {
                if (e.hasAttribute("configuration")) {
                    Element newElementMap = (Element) e.cloneNode(true)
                    newElementMap.setAttribute("pid", projectToCloneMapId.toString())
                    Element root = docMapProjectBehaviours.getDocumentElement()
                    root.appendChild(newElementMap)
                }

            }

        }
    }
}
private Document convertStringToXMLDocument(String xmlString) {
    //Parser that produces DOM object trees from XML content
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    //API to obtain DOM Document instance
    DocumentBuilder builder = null;
    try {
        //Create DocumentBuilder with default configuration
        builder = factory.newDocumentBuilder();

        //Parse the content to Document object
        Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
        return doc;
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}