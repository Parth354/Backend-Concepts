import { useState ,useEffect, useRef } from 'react';
import { Play, RotateCcw, Sliders, Activity, Send } from 'lucide-react';
import { useToast } from '../components/ToastProvider';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

export default function ExponentialBackoff() {
    const { error } = useToast();
    const pollingStoppedRef = useRef(false);
    const [statusCode ,setStatusCode] = useState(500);
    const [delayMs , setDelayMs] = useState(0);
    const [failuresBeforeSuccess , setFailuresBeforeSuccess] = useState(2);
    const [logs, setLogs] = useState([]);
    const [isConfiguring , setIsConfiguring ] = useState(false);

    const updateReceiverConfig = async () => {
        setIsConfiguring(true);
        try {
            await fetch(`${API_BASE}/mock-receiver/configure` , {
                method: 'POST',
                headers: { 'Content-Type' : 'application/json'},
                body : JSON.stringify({
                    statusCode:parseInt(statusCode),
                    delayMs: parseInt(delayMs),
                    failuresBeforeSuccess : parseInt(failuresBeforeSuccess),
                }),
            });
        } catch (err) {
            console.error('Failed to configure receiver' ,err );
            error('Unable to reach the mock receiver service. Please check the backend URL and connection.');
        } finally {
            setIsConfiguring(false);
        }
    };

    const triggerWebhook = async () => {
        try {
            const receiverUrl = `${API_BASE}/mock-receiver`;
            await fetch(`${API_BASE}/webhooks/trigger?url=${encodeURIComponent(receiverUrl)}`,
                {
                    method:'POST'
                }
             );
        } catch (err ) {
            console.error('Failed to trigger webhook' , err)
            error('Unable to trigger the webhook because the backend service is unreachable.');
        }
    };

    const fetchLogs = async () =>{
        if (pollingStoppedRef.current) return;

        try{
            const res = await fetch(`${API_BASE}/webhooks/logs`);
            if (!res.ok) {
                throw new Error(`Request failed with status ${res.status}`);
            }

            const data = await res.json();
            setLogs(data);
        }catch(err) {
            console.error('Error fetching logs : ' ,err);
            if (!pollingStoppedRef.current) {
                error('Unable to load execution logs from the backend service.');
            }
            pollingStoppedRef.current = true;
        }
    };

    const clearLogs = async () => {
        try{
            await fetch(`${API_BASE}/webhooks/logs` , {method : 'DELETE'});
            setLogs([]);
        } catch(err) {
            console.error('Error clearing logs : ' ,err);
            error('Unable to clear execution logs because the backend service is unreachable.');
        }
    };

    useEffect (() => {
        let intervalId;

        const pollLogs = async () => {
            if (pollingStoppedRef.current) {
                if (intervalId) clearInterval(intervalId);
                return;
            }

            await fetchLogs();
        };

        pollLogs();
        intervalId = setInterval(pollLogs, 500);
        return () => clearInterval(intervalId);
    }, [error]);

    return (
        <div className="page-container">
            <div className="page-header">
                <div>
                    <h1>Exponential Backoff Engine</h1>
                    <p>Simulate client failure recovery, network timeouts, and idempotency handling.</p>
                </div>
                <button className="btn-primary" onClick={triggerWebhook}> 
                    <Send size={16}/>Dispatch Webhook
                </button>
            </div>

            <div className="dashboard-grid">
                <div className="card">
                    <div className="card-header">
                        <Sliders size ={18} />
                        <h2>Receiver Behavior Configuration</h2>
                    </div>

                    <div className="form-group">
            <label>Response Status Code</label>
            <select value={statusCode} onChange={(e) => setStatusCode(e.target.value)}>
              <option value="500">500 Internal Server Error (Retryable)</option>
              <option value="429">429 Rate Limited (Respects Retry-After)</option>
              <option value="404">404 Not Found (Fatal - Aborts Immediately)</option>
              <option value="400">400 Bad Request (Fatal - Aborts Immediately)</option>
              <option value="200">200 OK (Success)</option>
            </select>
          </div>

          <div className="form-group">
            <label>Artificial Latency / Delay (ms)</label>
            <input
              type="number"
              value={delayMs}
              onChange={(e) => setDelayMs(e.target.value)}
              placeholder="e.g. 3000 to force backend timeout"
            />
            <span className="help-text">Values &gt; 2000ms trigger Spring Boot read timeout.</span>
          </div>

          <div className="form-group">
            <label>Failure Count Before Success</label>
            <input
              type="number"
              value={failuresBeforeSuccess}
              onChange={(e) => setFailuresBeforeSuccess(e.target.value)}
            />
          </div>

          <button className="btn-secondary" onClick={updateReceiverConfig} disabled={isConfiguring}>
            {isConfiguring ? 'Updating...' : 'Apply Rules to Mock Server'}
          </button>
        </div>

        {/* Live Execution Timeline Card */}
        <div className="card">
          <div className="card-header flex-between">
            <div className="flex-align">
              <Activity size={18} />
              <h2>Execution Log Feed</h2>
            </div>
            <button className="btn-ghost" onClick={clearLogs}>
              <RotateCcw size={14} /> Clear
            </button>
          </div>

          <div className="timeline-feed">
            {logs.length === 0 ? (
              <div className="empty-state">No execution events captured yet.</div>
            ) : (
              logs.slice().reverse().map((log, idx) => (
                <div key={idx} className={`timeline-card ${log.status}`}>
                  <div className="timeline-header">
                    <span className="timestamp">[{log.timestamp}] Attempt #{log.attempt}</span>
                    <span className={`status-badge ${log.status}`}>{log.status}</span>
                  </div>
                  <div className="timeline-body">{log.message}</div>
                  {log.delayMs > 0 && (
                    <div className="timeline-delay">
                      ⏳ Scheduled delay: <strong>{log.delayMs} ms</strong>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
)
}