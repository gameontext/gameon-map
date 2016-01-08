FROM websphere-liberty:beta

MAINTAINER Ben Smith

ADD https://download.elastic.co/logstash-forwarder/binaries/logstash-forwarder_linux_amd64 /opt/forwarder
ADD ./servers/gameon-map /opt/ibm/wlp/usr/servers/defaultServer/
RUN /opt/ibm/wlp/bin/installUtility install  --acceptLicense /opt/ibm/wlp/usr/servers/defaultServer/server.xml

COPY ./forwarder.conf /opt/forwarder.conf 	

COPY ./startup.sh /opt/startup.sh

CMD ["/opt/startup.sh"]

EXPOSE 9081
