package steffan.springmqdemoapp.sampleservice.services.interfaces

import steffan.springmqdemoapp.api.bindings.GreetingRequest

interface Greeter {

    fun greet(greetingRequest: GreetingRequest)

}
