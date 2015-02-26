package gc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import ot.FakeOTReceiver;
import ot.OTExtReceiver;
import ot.OTPreprocessReceiver;
import ot.OTReceiver;
import flexsc.Flag;
import flexsc.Party;

public abstract class GCEvaComp extends GCCompEnv{

	OTReceiver rcv;

	protected long gid = 0;

	public GCEvaComp(InputStream is, OutputStream os) {
		super(is, os, Party.Bob);

		if (Flag.FakeOT)
			rcv = new FakeOTReceiver(is, os);
		else if (Flag.ProprocessOT)
			rcv = new OTPreprocessReceiver(is, os);
		else
			rcv = new OTExtReceiver(is, os);
		
	}

	public GCSignal inputOfAlice(boolean in) {
		Flag.sw.startOT();
		GCSignal signal = GCSignal.receive(is);
		Flag.sw.stopOT();
		return signal;
	}

	public GCSignal inputOfBob(boolean in) {
		Flag.sw.startOT();
		GCSignal signal = null;
		try {
			signal = rcv.receive(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Flag.sw.stopOT();
		return signal;
	}

	public GCSignal[] inputOfBob(boolean[] x) {
		GCSignal[] ret = new GCSignal[x.length];
		for(int i = 0; i < x.length; i+=Flag.OTBlockSize) {
			GCSignal[] tmp = inputOfBobInter(Arrays.copyOfRange(x, i, Math.min(i+Flag.OTBlockSize, x.length)));
			System.arraycopy(tmp, 0, ret, i, tmp.length);
		}
		return ret;
	}
	
	public GCSignal[] inputOfBobInter(boolean[] x) {
		Flag.sw.startOT();
		GCSignal[] signal = null;
		try {
			signal = rcv.receive(x);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Flag.sw.stopOT();
		return signal;
	}

	public GCSignal[] inputOfAlice(boolean[] x) {
		Flag.sw.startOT();
		GCSignal[] result = new GCSignal[x.length];
		for (int i = 0; i < x.length; ++i)
			result[i] = GCSignal.receive(is);
		Flag.sw.stopOT();
		return result;
	}

	public boolean outputToAlice(GCSignal out) {
		if (!out.isPublic())
			out.send(os);
		return false;
	}

	public boolean outputToBob(GCSignal out) {
		if (out.isPublic())
			return out.v;

		GCSignal lb = GCSignal.receive(is);
		if (lb.equals(out))
			return false;
		// else if (lb.equals(R.xor(out)))
		else
			return true;
	}

	public boolean[] outputToAlice(GCSignal[] out) {
		boolean[] result = new boolean[out.length];

		for (int i = 0; i < result.length; ++i) {
			if (!out[i].isPublic())
				out[i].send(os);
		}
		try {
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < result.length; ++i)
			result[i] = false;
		return result;
	}

	public boolean[] outputToBob(GCSignal[] out) {
		boolean[] result = new boolean[out.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = outputToBob(out[i]);
		}
		return result;
	}

	public GCSignal xor(GCSignal a, GCSignal b) {
		if (a.isPublic() && b.isPublic())
			return  ((a.v ^ b.v) ?_ONE:_ZERO);
		else if (a.isPublic())
			return a.v ? not(b) : b;
		else if (b.isPublic())
			return b.v ? not(a) : a;
		else
			return a.xor(b);
	}

	public GCSignal not(GCSignal a) {
		if (a.isPublic())
			return ((!a.v) ?_ONE:_ZERO);//new GCSignal(!a.v);
		else {
			return a;//new GCSignal(a);
		}
	}
}
