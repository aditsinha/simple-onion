
CLASSPATH = junit-4.12.jar:bcprov-jdk15on.jar:./

JFLAGS = -g -classpath $(CLASSPATH)

JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $<



CLASSES = \
	com/onion/OnionMessage \
	com/onion/CircuitHopReplyMessage \
	com/onion/CircuitHopRequestMessage \
	com/onion/CipherUtils \
	com/onion/CircuitHopKeyReply \
	com/onion/CircuitEstablishment \
	com/onion/App \
	com/onion/CircuitHopKeyRequest \
	com/onion/CircuitHopKeyResponse

SRCS = $(addsuffix .java, $(CLASSES))
OBJS = $(addsuffix .class, $(CLASSES))

all: onion.jar

onion.jar: $(OBJS)
	find -name "*.class" | xargs jar cfe onion.jar com.onion.App

classes: $(OBJS)

%.o : %.c ; $(JC) $(JFLAGS) $<

clean:
	find . -name "*.class" | tee /dev/stderr | xargs rm -f
	rm -f *.jar

launch: all
	$(JLAUNCH) $(ARCH) $(SERVER_FLAGS)

