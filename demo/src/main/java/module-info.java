import com.github.rcubedev.demo.ServiceDemo;

module com.github.rcubedev.demo {
    requires org.slf4j;
    requires com.github.rcubedev.utils;

    exports com.github.rcubedev.demo;

    provides ServiceDemo.MyService with ServiceDemo.MyServiceImpl;
    uses ServiceDemo.MyService;
}