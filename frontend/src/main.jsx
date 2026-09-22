import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import Barcode from "react-barcode";
import {
  Leaf,
  LayoutDashboard,
  Sprout,
  FlaskConical,
  PackageOpen,
  RefreshCw,
  Trash2,
  LogOut,
  Users,
  BadgeDollarSign,
  ShoppingCart,
  BarChart3,
  Pencil,
} from "lucide-react";
import "./styles.css";
import "./staff.css";
import "./dashboard.css";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});
const nav = [
  ["Dashboard", LayoutDashboard],
  ["Analytics", BarChart3],
  ["Plants", Sprout],
  ["Media Compositions", FlaskConical],
  ["Media Bottles", PackageOpen],
  ["Plant Initiation", PackageOpen],
  ["Subcultures", RefreshCw],
  ["Discards", Trash2],
  ["Plant Exit", BadgeDollarSign],
  ["Price List", BadgeDollarSign],
  ["Sales / POS", ShoppingCart],
  ["Staff", Users],
];
const accessOptions = [
  ["Dashboard", "ACCESS_DASHBOARD"],
  ["Plants", "ACCESS_PLANTS"],
  ["Media Bottles", "ACCESS_MEDIA"],
  ["Plant Initiation", "ACCESS_MOTHER_BOTTLES"],
  ["Subcultures", "ACCESS_SUBCULTURES"],
  ["Discards", "ACCESS_DISCARDS"],
  ["Plant Exit", "ACCESS_PLANT_EXIT"],
  ["Price List", "ACCESS_PRICE_LIST"],
  ["Sales / POS", "ACCESS_SALES"],
];
const hormoneOptions = [
  "BAP",
  "2,4-D",
  "2iP",
  "IBA",
  "IAA",
  "Ascorbic Acid",
  "Gibberellic Acid",
  "Adenine Sulfate",
];

function currentIsoDate() {
  const date = new Date(),
    utc = new Date(
      Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()),
    );
  utc.setUTCDate(utc.getUTCDate() + 4 - (utc.getUTCDay() || 7));
  const year = utc.getUTCFullYear(),
    yearStart = new Date(Date.UTC(year, 0, 1));
  return {
    date: date.toLocaleDateString("en-CA"),
    year,
    week: Math.ceil(((utc - yearStart) / 86400000 + 1) / 7),
  };
}

function Login({ onLogin }) {
  const [username, setUsername] = useState(""),
    [password, setPassword] = useState(""),
    [error, setError] = useState("");
  async function submit(e) {
    e.preventDefault();
    const token = btoa(`${username.trim()}:${password}`);
    api.defaults.headers.common.Authorization = `Basic ${token}`;
    try {
      const { data } = await api.get("/me");
      sessionStorage.setItem("labAuth", token);
      onLogin(data);
    } catch {
      delete api.defaults.headers.common.Authorization;
      setError("Username or password is incorrect.");
    }
  }
  return (
    <main className="login">
      <form className="login-card" onSubmit={submit} autoComplete="off">
        <div className="brand-mark">
          <Leaf />
        </div>
        <p className="eyebrow">NATURAL FOLIAGE</p>
        <h1>Lab Management</h1>
        <p className="muted">Sign in to manage cultures and inventory.</p>
        {error && <div className="error">{error}</div>}
        <label>
          Username
          <input
            value={username}
            autoComplete="off"
            required
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            autoComplete="off"
            required
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button>Sign in</button>
      </form>
    </main>
  );
}

function LaminaFlowSetup({ user, onSelect }) {
  const [value, setValue] = useState("LF-01");
  return (
    <main className="login">
      <form
        className="login-card"
        onSubmit={(e) => {
          e.preventDefault();
          onSelect(value);
        }}
      >
        <div className="brand-mark">
          <RefreshCw />
        </div>
        <p className="eyebrow">SUBCULTURE SESSION</p>
        <h1>Hello, {user.fullName}</h1>
        <p className="muted">
          Select the lamina flow cabinet you are using for this login session.
        </p>
        <label>
          Lamina flow
          <select value={value} onChange={(e) => setValue(e.target.value)}>
            <option>LF-01</option>
            <option>LF-02</option>
            <option>LF-03</option>
            <option>LF-04</option>
          </select>
        </label>
        <button>Start session</button>
      </form>
    </main>
  );
}

function OperationalTables({ details }) {
  const [expandedOldGroups, setExpandedOldGroups] = useState([]);
  function toggleOldGroup(groupKey) {
    setExpandedOldGroups((current) =>
      current.includes(groupKey)
        ? current.filter((key) => key !== groupKey)
        : [...current, groupKey],
    );
  }
  return (
    <section className="dashboard-tables">
      <article className="panel table-wrap dashboard-table">
        <div className="table-heading">
          <div>
            <p className="eyebrow">LIVE INVENTORY</p>
            <h2>Available plants</h2>
          </div>
          <span className="count-pill">
            {details.availablePlants.length} varieties
          </span>
        </div>
        <div className="dashboard-table-scroll"><table>
          <thead>
            <tr>
              <th>Plant code</th>
              <th>Plant name</th>
              <th>Multiply</th>
              <th>Rooting</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            {details.availablePlants.map((row) => (
              <tr key={row.plantCode}>
                <td>
                  <b>{row.plantCode}</b>
                </td>
                <td>{row.plantName}</td>
                <td>{row.multiply}</td>
                <td>{row.rooting}</td>
                <td>
                  <b>{row.total}</b>
                </td>
              </tr>
            ))}
            {!details.availablePlants.length && (
              <tr>
                <td colSpan="5" className="empty">
                  No active subculture plants yet.
                </td>
              </tr>
            )}
          </tbody>
        </table></div>
      </article>
      <article className="panel table-wrap dashboard-table warning-table">
        <div className="table-heading">
          <div>
            <p className="eyebrow warning">ATTENTION REQUIRED</p>
            <h2>Subculture bottles older than 6 weeks</h2>
          </div>
          <span className="count-pill warning">
            {details.oldCultures.length} groups
          </span>
        </div>
        <div className="dashboard-table-scroll"><table>
          <thead>
            <tr>
              <th>Week</th>
              <th>Code</th>
              <th>Variety</th>
              <th>Bottles</th>
              <th>Total plants</th>
              <th>Age</th>
            </tr>
          </thead>
          <tbody>
            {details.oldCultures.map((row, index) => {
              const groupKey = `${row.plantCode}-${row.subcultureWeek}-${row.ageWeeks}-${index}`;
              const expanded = expandedOldGroups.includes(groupKey);
              return <React.Fragment key={groupKey}><tr>
                <td>{row.subcultureWeek || "—"}</td>
                <td>
                  <b>{row.plantCode}</b>
                </td>
                <td>{row.variety}</td>
                <td><button type="button" className="bottle-count-button" onClick={() => toggleOldGroup(groupKey)} aria-expanded={expanded}>{row.bottles} {expanded ? "Hide" : "View"}</button></td>
                <td>{row.totalPlants}</td>
                <td>
                  <span className="age-alert">{row.ageWeeks} weeks</span>
                </td>
              </tr>{expanded && <tr className="barcode-detail-row"><td colSpan="6"><b>Bottles to subculture</b><div className="overdue-barcode-list">{(row.barcodes || []).map((barcode) => <code key={barcode}>{barcode}</code>)}</div></td></tr>}</React.Fragment>;
            })}
            {!details.oldCultures.length && (
              <tr>
                <td colSpan="6" className="empty healthy">
                  No overdue subculture bottles.
                </td>
              </tr>
            )}
          </tbody>
        </table></div>
      </article>
    </section>
  );
}

function AnalyticsChart({ data, type, metricLabel, chartRef }) {
  if (!data.length) return <div className="empty">No matching discard data yet.</div>;
  const width = Math.max(720, data.length * 92);
  const height = 360;
  const margin = { top: 28, right: 24, bottom: 82, left: 58 };
  const plotWidth = width - margin.left - margin.right;
  const plotHeight = height - margin.top - margin.bottom;
  const maximum = Math.max(1, ...data.map((item) => item.value));
  const y = (value) => margin.top + plotHeight - (value / maximum) * plotHeight;
  const x = (index) =>
    data.length === 1
      ? margin.left + plotWidth / 2
      : margin.left + (index / (data.length - 1)) * plotWidth;
  const linePoints = data.map((item, index) => `${x(index)},${y(item.value)}`).join(" ");
  const ticks = [...new Set([0, 1, 2, 3, 4].map((step) => Math.round((maximum * step) / 4)))];
  const slotWidth = plotWidth / data.length;
  const barWidth = Math.min(48, slotWidth * 0.62);
  return (
    <div className="analytics-chart-scroll">
      <svg ref={chartRef} className="analytics-chart" viewBox={`0 0 ${width} ${height}`} width={width} height={height} role="img" aria-label={`${type} chart of ${metricLabel}`} xmlns="http://www.w3.org/2000/svg">
        <rect width={width} height={height} fill="#ffffff" />
        <text x={margin.left} y="16" fill="#496158" fontSize="11" fontWeight="700">{metricLabel}</text>
        {ticks.map((tick) => {
          const tickY = y(tick);
          return <g key={tick}><line x1={margin.left} x2={width - margin.right} y1={tickY} y2={tickY} stroke="#e6eeea" /><text x={margin.left - 10} y={tickY + 4} textAnchor="end" fill="#708079" fontSize="10">{tick}</text></g>;
        })}
        {type === "line" ? <>
          <polygon points={`${margin.left},${margin.top + plotHeight} ${linePoints} ${x(data.length - 1)},${margin.top + plotHeight}`} fill="#64aa8a22" />
          {data.length > 1 && <polyline points={linePoints} fill="none" stroke="#277657" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />}
          {data.map((item, index) => <g key={item.label}><circle cx={x(index)} cy={y(item.value)} r="5" fill="#277657" stroke="#fff" strokeWidth="2" /><text x={x(index)} y={y(item.value) - 10} textAnchor="middle" fill="#214f3b" fontSize="10" fontWeight="700">{item.value}</text></g>)}
        </> : data.map((item, index) => {
          const barX = margin.left + slotWidth * index + (slotWidth - barWidth) / 2;
          const barY = y(item.value);
          return <g key={item.label}><rect x={barX} y={barY} width={barWidth} height={margin.top + plotHeight - barY} rx="5" fill="#3a8b69" /><text x={barX + barWidth / 2} y={barY - 8} textAnchor="middle" fill="#214f3b" fontSize="10" fontWeight="700">{item.value}</text></g>;
        })}
        {data.map((item, index) => {
          const labelX = type === "bar" ? margin.left + slotWidth * index + slotWidth / 2 : x(index);
          return <text key={item.label} x={labelX} y={height - margin.bottom + 22} transform={`rotate(-35 ${labelX} ${height - margin.bottom + 22})`} textAnchor="end" fill="#5c7067" fontSize="10">{item.label}</text>;
        })}
      </svg>
    </div>
  );
}

function AnalyticsPage() {
  const [records, setRecords] = useState([]),
    [xAxis, setXAxis] = useState("month"),
    [yAxis, setYAxis] = useState("plants"),
    [reasonFilter, setReasonFilter] = useState("All"),
    [chartType, setChartType] = useState("auto"),
    [error, setError] = useState("");
  const chartRef = useRef(null);

  useEffect(() => {
    api
      .get("/discards")
      .then((response) => setRecords(response.data))
      .catch((requestError) =>
        setError(requestError.response?.data?.message || "Could not load analytics data."),
      );
  }, []);

  const filtered = records.filter(
    (item) =>
      reasonFilter === "All" ||
      (item.reason || "").toLowerCase().includes(reasonFilter.toLowerCase()),
  );
  function dimensionValues(item) {
    const date = new Date(`${item.discardedDate}T00:00:00`);
    const dimensions = {
      laminaFlow: item.sourceLaminaFlow || "Unknown / legacy data",
      technician: item.sourceTechnician || "Unknown",
      discardedBy: item.technician || "Unknown",
      year: String(date.getFullYear()),
      month: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      plant: item.plantCode || "Unknown",
      cycle: `Cycle ${item.cycle}`,
      week: `${Number.isNaN(date.getFullYear()) ? "Unknown" : date.getFullYear()}-W${String(item.cultureWeek || 0).padStart(2, "0")}`,
    };
    if (xAxis === "reason") {
      return (item.reason || "Unknown").split(",").map((reason) => reason.trim()).filter(Boolean);
    }
    return [dimensions[xAxis]];
  }
  const grouped = new Map();
  filtered.forEach((item) => {
    dimensionValues(item).forEach((label) => {
      const amount = yAxis === "plants" ? Number(item.plantCount || 0) : 1;
      grouped.set(label, (grouped.get(label) || 0) + amount);
    });
  });
  const chartData = [...grouped.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) =>
      ["year", "month", "week", "cycle"].includes(xAxis)
        ? a.label.localeCompare(b.label, undefined, { numeric: true })
        : b.value - a.value,
    );
  const totalPlants = filtered.reduce(
    (total, item) => total + Number(item.plantCount || 0),
    0,
  );
  const temporalAxis = ["year", "month", "week"].includes(xAxis);
  const effectiveChartType = chartType === "auto" ? (temporalAxis ? "line" : "bar") : chartType;
  const metricLabel = yAxis === "plants" ? "Number of plants affected" : "Number of bottles discarded";
  const trend = temporalAxis && chartData.length > 1
    ? chartData.at(-1).value - chartData[0].value
    : null;
  const trendPercent = trend !== null && chartData[0].value > 0
    ? Math.abs((trend / chartData[0].value) * 100).toFixed(1)
    : null;
  function safeFilename(value) {
    return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
  }
  function downloadChart() {
    if (!chartRef.current) return;
    const copy = chartRef.current.cloneNode(true);
    copy.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    const source = new XMLSerializer().serializeToString(copy);
    const url = URL.createObjectURL(new Blob([source], { type: "image/svg+xml;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `discard-analysis-${safeFilename(xAxis)}.svg`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  function downloadCsv() {
    const escape = (value) => `"${String(value).replaceAll('"', '""')}"`;
    const rows = [["Category", metricLabel], ...chartData.map((item) => [item.label, item.value])];
    const content = rows.map((row) => row.map(escape).join(",")).join("\n");
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `discard-analysis-${safeFilename(xAxis)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  return (
    <>
      <header>
        <p className="eyebrow">ADMIN ANALYTICS</p>
        <h1>Laboratory analytics</h1>
        <p className="muted">Choose an X-axis and Y-axis to generate the graph automatically.</p>
      </header>
      {error && <div className="error">{error}</div>}
      <section className="analytics-summary">
        <article className="stat"><span>Discarded bottles</span><strong>{filtered.length}</strong></article>
        <article className="stat discard"><span>Number of plants affected</span><strong>{totalPlants}</strong></article>
      </section>
      <section className="panel analytics-panel">
        <div className="chart-controls">
          <label>Reason filter<select value={reasonFilter} onChange={(event) => setReasonFilter(event.target.value)}><option>All</option><option>Bacterial</option><option>Fungal</option><option>Mites</option><option>Plant dead</option><option>Other</option></select></label>
          <label>X-axis<select value={xAxis} onChange={(event) => setXAxis(event.target.value)}><option value="month">Year / month</option><option value="year">Year</option><option value="laminaFlow">Lamina flow</option><option value="technician">Subculture technician</option><option value="discardedBy">Discarded by</option><option value="plant">Plant variety</option><option value="reason">Discard reason</option><option value="cycle">Culture cycle</option><option value="week">Culture week</option></select></label>
          <label>Y-axis<select value={yAxis} onChange={(event) => setYAxis(event.target.value)}><option value="plants">Number of plants affected</option><option value="bottles">Number of bottles discarded</option></select></label>
          <label>Chart type<select value={chartType} onChange={(event) => setChartType(event.target.value)}><option value="auto">Automatic (recommended)</option><option value="line">Line chart</option><option value="bar">Bar chart</option></select></label>
        </div>
        <div className="chart-title"><div><h2>{reasonFilter} discards by {xAxis === "laminaFlow" ? "lamina flow" : xAxis}</h2><span>{metricLabel}</span></div><div className="chart-downloads"><button type="button" className="secondary compact" onClick={downloadCsv} disabled={!chartData.length}>Download data (CSV)</button><button type="button" onClick={downloadChart} disabled={!chartData.length}>Download chart (SVG)</button></div></div>
        <div className={`trend-summary ${trend > 0 ? "up" : trend < 0 ? "down" : "stable"}`}>
          {temporalAxis ? chartData.length > 1 ? <><b>{trend > 0 ? "Increasing" : trend < 0 ? "Decreasing" : "Stable"}</b><span>{trend === 0 ? `Latest period is the same as the first period (${chartData.at(-1).value}).` : `Latest period is ${Math.abs(trend)} ${metricLabel.toLowerCase()} ${trend > 0 ? "higher" : "lower"} than the first period${trendPercent ? ` (${trendPercent}%)` : ""}.`}</span></> : <span>Add data from at least two periods to calculate an increase/decrease trend.</span> : chartData.length ? <><b>Highest category</b><span>{chartData[0].label}: {chartData[0].value} {metricLabel.toLowerCase()}</span></> : <span>No matching data to analyse.</span>}
        </div>
        <AnalyticsChart data={chartData} type={effectiveChartType} metricLabel={metricLabel} chartRef={chartRef} />
      </section>
    </>
  );
}

function Dashboard({ user }) {
  const [details, setDetails] = useState({ availablePlants: [], oldCultures: [] }),
    [mediaStock, setMediaStock] = useState([]),
    [mediaExpanded, setMediaExpanded] = useState(false),
    [rootedExpanded, setRootedExpanded] = useState(false);
  useEffect(() => {
    api.get("/dashboard/details").then((response) => setDetails(response.data));
    async function loadMediaStock() {
      try {
        let response;
        try {
          response = await api.get("/media/options");
        } catch (requestError) {
          if (
            requestError.response?.status !== 404 &&
            requestError.response?.status !== 405
          ) {
            throw requestError;
          }
          response = await api.get("/media");
        }
        setMediaStock(response.data);
      } catch {
        setMediaStock([]);
      }
    }
    loadMediaStock();
  }, []);
  const tech = user.role === "TECHNICIAN";
  const availableMediaBottles = mediaStock.reduce(
    (total, item) => total + Number(item.availableBottles || 0),
    0,
  );
  const rootedStock = details.availablePlants.filter((item) => item.rooting > 0);
  const rootedTotal = rootedStock.reduce(
    (total, item) => total + Number(item.rooting || 0),
    0,
  );
  const sellableTotal = rootedStock.reduce(
    (total, item) => total + Math.floor(Number(item.rooting || 0) * 0.8),
    0,
  );
  const totalActivePlants = details.availablePlants.reduce(
    (total, item) => total + Number(item.total || 0),
    0,
  );
  const cards = [
    ["Available media bottles", availableMediaBottles, "media"],
    ["Rooted plants", rootedTotal, "rooted"],
    ["Total active plants / clumps", totalActivePlants, "plants-total"],
  ];
  return (
    <>
      <header className="dashboard-header">
        <div><p className="eyebrow">{tech ? "MY WORKSPACE" : "OVERVIEW"}</p>
          <h1>{tech ? "Technician dashboard" : "Laboratory dashboard"}</h1>
          <p className="muted">{tech ? `${user.fullName} · ${user.employeeId} · ${user.labSection}` : "A quick view of today’s tissue-culture inventory."}</p>
        </div>
      </header>
      <section className="cards dashboard-cards">
        {cards.map(([l, v, c]) => (
          <article
            className={`stat ${c} ${c === "media" || c === "rooted" ? "expandable" : ""}`}
            key={l}
            onClick={
              c === "media" ? () => setMediaExpanded((open) => !open) : c === "rooted" ? () => setRootedExpanded((open) => !open) : undefined
            }
            onKeyDown={
              c === "media" || c === "rooted"
                ? (event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      if (c === "media") setMediaExpanded((open) => !open);
                      if (c === "rooted") setRootedExpanded((open) => !open);
                    }
                  }
                : undefined
            }
            role={c === "media" || c === "rooted" ? "button" : undefined}
            tabIndex={c === "media" || c === "rooted" ? 0 : undefined}
            aria-expanded={c === "media" ? mediaExpanded : c === "rooted" ? rootedExpanded : undefined}
          >
            <span>{l}</span>
            <strong>{v ?? "—"}</strong>
            {c === "media" && (
              <small className="stock-toggle">
                {mediaExpanded ? "Hide composition details" : "View composition details"}
              </small>
            )}
            {c === "rooted" && (
              <small className="sellable-summary">
                Sellable 80%: <b>{sellableTotal}</b> · {rootedExpanded ? "Hide details" : "View details"}
              </small>
            )}
            {c === "media" && mediaExpanded && mediaStock.length > 0 && (
              <div className="media-stock-list">
                {mediaStock.map((item) => (
                  <div key={item.id}>
                    <b>{item.code}</b>
                    <span>{item.availableBottles} bottles</span>
                  </div>
                ))}
              </div>
            )}
            {c === "rooted" && rootedExpanded && rootedStock.length > 0 && (
              <div className="media-stock-list rooted-stock-list">{rootedStock.map((item) => <div key={item.plantCode}><span><b>{item.plantCode}</b> · {item.plantName}</span><span>{item.rooting} rooted · {Math.floor(item.rooting * 0.8)} sellable</span></div>)}</div>
            )}
          </article>
        ))}
      </section>
      <OperationalTables details={details} />
    </>
  );
}

function HormoneMultiSelect({ value, onChange }) {
  const segments = (value || "").split(";").map((item) => item.trim()).filter(Boolean);
  const parseSegment = (segment) => {
    const match = segment.match(/^(.*?)(?:\s+([0-9]*\.?[0-9]+)\s*mg\/L)?$/i);
    return { name: match?.[1]?.trim() || "", quantity: match?.[2] || "" };
  };
  const parsed = segments.map(parseSegment);
  const fixedEntry = (option) => parsed.find((item) => item.name.toLowerCase() === option.toLowerCase());
  const selected = hormoneOptions.filter((option) => fixedEntry(option));
  const quantity = (option) => fixedEntry(option)?.quantity || "";
  const savedOther = parsed.find((item) => !hormoneOptions.some((option) => option.toLowerCase() === item.name.toLowerCase()));
  const [otherEnabled, setOtherEnabled] = useState(Boolean(savedOther));
  const [otherName, setOtherName] = useState(savedOther?.name || "");
  const [otherQuantity, setOtherQuantity] = useState(savedOther?.quantity || "");
  useEffect(() => {
    setOtherEnabled(Boolean(savedOther));
    setOtherName(savedOther?.name || "");
    setOtherQuantity(savedOther?.quantity || "");
  }, [value]);
  function serialize(options, customName = otherName, customQuantity = otherQuantity) {
    const fixed = options.map((option) => quantity(option) ? `${option} ${quantity(option)} mg/L` : option);
    if (customName.trim()) fixed.push(customQuantity ? `${customName.trim()} ${customQuantity} mg/L` : customName.trim());
    return fixed.join("; ");
  }
  function toggle(option) {
    onChange(serialize(selected.includes(option) ? selected.filter((item) => item !== option) : [...selected, option]));
  }
  function setQuantity(option, amount) {
    const fixed = selected.map((item) => item === option ? (amount ? `${item} ${amount} mg/L` : item) : quantity(item) ? `${item} ${quantity(item)} mg/L` : item);
    if (otherName.trim()) fixed.push(otherQuantity ? `${otherName.trim()} ${otherQuantity} mg/L` : otherName.trim());
    onChange(fixed.join("; "));
  }
  function toggleOther() {
    if (otherEnabled) {
      setOtherEnabled(false); setOtherName(""); setOtherQuantity("");
      onChange(serialize(selected, "", ""));
    } else setOtherEnabled(true);
  }
  function updateOther(name, amount) {
    setOtherName(name); setOtherQuantity(amount);
    onChange(serialize(selected, name, amount));
  }
  const summaries = selected.map((option) => quantity(option) ? `${option} ${quantity(option)} mg/L` : option);
  if (otherEnabled) summaries.push(otherName ? `${otherName}${otherQuantity ? ` ${otherQuantity} mg/L` : ""}` : "Other (enter name)");
  return (
    <details className="multi-select">
      <summary>
        {summaries.length
          ? summaries.join(", ")
          : "Select one or more hormones"}
      </summary>
      <div className="multi-select-menu">
        {hormoneOptions.map((option) => (
          <div
            className={`hormone-row ${selected.includes(option) ? "selected" : ""}`}
            key={option}
          >
            <label>
              <input
                type="checkbox"
                checked={selected.includes(option)}
                onChange={() => toggle(option)}
              />
              <span>{option}</span>
            </label>
            {selected.includes(option) && (
              <div className="quantity-field">
                <input
                  aria-label={`${option} quantity`}
                  type="number"
                  min="0"
                  step="0.001"
                  placeholder="0.000"
                  value={quantity(option)}
                  onChange={(e) => setQuantity(option, e.target.value)}
                  required
                />
                <span>mg/L</span>
              </div>
            )}
          </div>
        ))}
        <div className={`hormone-row other-hormone-row ${otherEnabled ? "selected" : ""}`}>
          <label><input type="checkbox" checked={otherEnabled} onChange={toggleOther} /><span>Other</span></label>
          {otherEnabled && <div className="other-hormone-fields"><input aria-label="Other hormone name" placeholder="Hormone name" value={otherName} onChange={(e) => updateOther(e.target.value, otherQuantity)} required /><div className="quantity-field"><input aria-label="Other hormone quantity" type="number" min="0" step="0.001" placeholder="0.000" value={otherQuantity} onChange={(e) => updateOther(otherName, e.target.value)} required /><span>mg/L</span></div></div>}
        </div>
      </div>
      {summaries.length > 0 && (
        <div className="selected-values">
          {selected.map((option) => (
            <span key={option}>
              {option}
              {quantity(option)
                ? ` · ${quantity(option)} mg/L`
                : " · quantity required"}
            </span>
          ))}
          {otherEnabled && <span>{otherName || "Other hormone name required"}{otherQuantity ? ` · ${otherQuantity} mg/L` : " · quantity required"}</span>}
        </div>
      )}
    </details>
  );
}

const configs = {
  Plants: {
    url: "/plants",
    title: "Plant catalogue",
    buttonLabel: "plant",
    fields: [
      ["code", "Plant code"],
      ["name", "Plant name"],
      ["variety", "Variety"],
      ["description", "Description"],
    ],
  },
  Media: {
    url: "/media",
    title: "Media compositions",
    buttonLabel: "media",
    fields: [
      ["code", "Media code"],
      ["basalMedia", "Basal media"],
      ["hormones", "Hormones"],
      ["ph", "pH", "number"],
      ["agar", "Agar (g/L)", "number"],
    ],
  },
  "Price List": {
    url: "/price-list",
    title: "Plant price list",
    buttonLabel: "price",
    fields: [
      ["plantCode", "Plant code"],
      ["varietyName", "Variety name"],
      ["price", "Unit price (LKR)", "number"],
    ],
  },
};
function CrudPage({ type, user }) {
  const c = configs[type],
    empty = Object.fromEntries(c.fields.map((f) => [f[0], ""]));
  const [rows, setRows] = useState([]),
    [form, setForm] = useState(empty),
    [editingId, setEditingId] = useState(null),
    [error, setError] = useState("");
  const load = () => api.get(c.url).then((r) => setRows(r.data));
  useEffect(() => {
    setForm(empty);
    setEditingId(null);
    load();
  }, [type]);
  async function save(e) {
    e.preventDefault();
    setError("");
    try {
      if (editingId) await api.put(`${c.url}/${editingId}`, form);
      else await api.post(c.url, form);
      setForm(empty);
      setEditingId(null);
      load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not save this record.");
    }
  }
  async function remove(id) {
    if (confirm("Delete this record?")) {
      await api.delete(`${c.url}/${id}`);
      if (editingId === id) {
        setEditingId(null);
        setForm(empty);
      }
      load();
    }
  }
  function edit(row) {
    setEditingId(row.id);
    setForm(Object.fromEntries(c.fields.map(([key]) => [key, row[key] ?? ""])));
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function cancelEdit() {
    setEditingId(null);
    setForm(empty);
    setError("");
  }
  return (
    <>
      <header>
        <p className="eyebrow">MASTER DATA</p>
        <h1>{c.title}</h1>
      </header>
      <section className="split">
        <form className="panel form" onSubmit={save}>
          <h2>{editingId ? `Edit ${c.buttonLabel}` : "Add new"}</h2>
          {error && <div className="error">{error}</div>}
          {c.fields.map(([key, label, t]) => (
            <label key={key}>
              {label}
              {type === "Media" && key === "hormones" ? (
                <HormoneMultiSelect
                  value={form.hormones}
                  onChange={(hormones) => setForm({ ...form, hormones })}
                />
              ) : (
                <input
                  type={t || "text"}
                  step="any"
                  value={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                  required={!(type === "Plants" && key === "description")}
                />
              )}
            </label>
          ))}
          <button>{editingId ? `Update ${c.buttonLabel}` : `Save ${c.buttonLabel}`}</button>
          {editingId && <button type="button" className="secondary" onClick={cancelEdit}>Cancel editing</button>}
        </form>
        <section className={`panel table-wrap ${type === "Plants" ? "plant-list-panel" : ""}`}>
          <table>
            <thead>
              <tr>
                {c.fields.slice(0, 3).map((f) => (
                  <th key={f[0]}>{f[1]}</th>
                ))}
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  {c.fields.slice(0, 3).map((f) => (
                    <td key={f[0]}>{r[f[0]] ?? "—"}</td>
                  ))}
                  <td>
                    {user.role === "ADMIN" && type === "Plants" && (
                      <div className="record-actions"><button type="button" className="icon edit" aria-label={`Edit ${r.code || r.name}`} onClick={() => edit(r)}><Pencil size={16} /></button><button type="button" className="icon danger" aria-label={`Delete ${r.code || r.name}`} onClick={() => remove(r.id)}><Trash2 size={16} /></button></div>
                    )}
                    {user.role === "ADMIN" && type !== "Plants" && <button type="button" className="icon danger" aria-label={`Delete ${r.code || r.name}`} onClick={() => remove(r.id)}><Trash2 size={16} /></button>}
                  </td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan="4" className="empty">
                    No records yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
      </section>
    </>
  );
}

function MediaCompositionPage() {
  const initial = {
    code: "",
    basalMedia: "",
    hormones: "",
    ph: "",
    agar: "",
  };
  const [rows, setRows] = useState([]),
    [form, setForm] = useState(initial),
    [editingId, setEditingId] = useState(null),
    [error, setError] = useState("");
  const load = () => api.get("/media").then((r) => setRows(r.data));
  useEffect(() => {
    load();
  }, []);
  async function save(e) {
    e.preventDefault();
    setError("");
    try {
      if (editingId) await api.put(`/media/${editingId}`, form);
      else await api.post("/media", form);
      setForm(initial);
      setEditingId(null);
      load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not save media.");
    }
  }
  function edit(item) {
    setEditingId(item.id);
    setForm({
      code: item.code || "",
      basalMedia: item.basalMedia || "",
      hormones: item.hormones || "",
      ph: item.ph ?? "",
      agar: item.agar ?? "",
    });
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function cancelEdit() {
    setEditingId(null);
    setForm(initial);
    setError("");
  }
  async function remove(item) {
    if (!confirm(`Delete media composition ${item.code}?`)) return;
    setError("");
    try {
      await api.delete(`/media/${item.id}`);
      if (editingId === item.id) cancelEdit();
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "This media composition cannot be deleted because it is already used by bottles or laboratory records.");
    }
  }
  return (
    <>
      <header>
        <p className="eyebrow">ADMIN · SECRET RECIPES</p>
        <h1>Media compositions</h1>
        <p className="muted">
          Maintain confidential laboratory recipes. This tab is available to
          administrators only.
        </p>
      </header>
      <section className="split">
        <form className="panel form media-composition-form" onSubmit={save}>
          <h2>{editingId ? "Edit media composition" : "Add media composition"}</h2>
          {error && <div className="error">{error}</div>}
          <label>
            Media code
            <input
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              required
            />
          </label>
          <label>
            Basal media
            <input
              value={form.basalMedia}
              onChange={(e) => setForm({ ...form, basalMedia: e.target.value })}
              required
            />
          </label>
          <label>
            Hormones
            <HormoneMultiSelect
              value={form.hormones}
              onChange={(hormones) => setForm({ ...form, hormones })}
            />
          </label>
          <label>
            pH
            <input
              type="number"
              step="0.01"
              value={form.ph}
              onChange={(e) => setForm({ ...form, ph: e.target.value })}
              required
            />
          </label>
          <label>
            Agar (g/L)
            <input
              type="number"
              step="0.01"
              value={form.agar}
              onChange={(e) => setForm({ ...form, agar: e.target.value })}
              required
            />
          </label>
          <button>{editingId ? "Update media" : "Save media"}</button>
          {editingId && <button type="button" className="secondary" onClick={cancelEdit}>Cancel editing</button>}
        </form>
        <section className="panel table-wrap media-composition-list">
          <table>
            <thead>
              <tr>
                <th>Code / recipe</th>
                <th>Hormones</th>
                <th>Available bottles</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.id}>
                  <td>
                    <b>{item.code}</b>
                    <small className="cell-sub">{item.basalMedia}</small>
                  </td>
                  <td>{item.hormones || "—"}</td>
                  <td>
                    <b>{item.availableBottles}</b>
                  </td>
                  <td><div className="record-actions"><button type="button" className="secondary compact" onClick={() => edit(item)}><Pencil size={14} /> Edit</button><button type="button" className="icon danger" aria-label={`Delete ${item.code}`} onClick={() => remove(item)}><Trash2 size={16} /></button></div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </section>
    </>
  );
}

function MediaBottlesPage({ user }) {
  const legacyHistoryKey = `media-preparations:${user.username}`;
  const [compositions, setCompositions] = useState([]),
    [rows, setRows] = useState([]),
    [mediaId, setMediaId] = useState(""),
    [bottleCount, setBottleCount] = useState(""),
    [ph, setPh] = useState(""),
    [agar, setAgar] = useState(""),
    [sterilizationMethod, setSterilizationMethod] = useState(""),
    [editingId, setEditingId] = useState(null),
    [error, setError] = useState("");
  async function load() {
    setError("");
    let loadedCompositions = [];

    // Load the two sections independently. This keeps the composition selector
    // usable when a previously started backend does not yet expose preparation
    // history, and the fallback supports that backend's media lookup route.
    try {
      let media;
      try {
        media = await api.get("/media/options");
      } catch (requestError) {
        if (requestError.response?.status !== 404 && requestError.response?.status !== 405) {
          throw requestError;
        }
        media = await api.get("/media");
      }
      loadedCompositions = media.data;
      setCompositions(loadedCompositions);
    } catch (requestError) {
      setCompositions([]);
      setError(
        requestError.response?.data?.message ||
          "Could not load saved media compositions.",
      );
    }

    try {
      const preparations = await api.get("/media-preparations");
      setRows(preparations.data);
    } catch (requestError) {
      if (requestError.response?.status !== 404 && requestError.response?.status !== 405) {
        setError((current) =>
          current ||
          requestError.response?.data?.message ||
          "Could not load media preparation history.",
        );
        setRows([]);
        return;
      }

      let savedRows = [];
      try {
        savedRows = JSON.parse(localStorage.getItem(legacyHistoryKey) || "[]");
      } catch {
        localStorage.removeItem(legacyHistoryKey);
      }
      const recordedMediaIds = new Set(
        savedRows.map((item) => String(item.media.id)),
      );
      const existingStockRows = loadedCompositions
        .filter(
          (item) =>
            item.availableBottles > 0 && !recordedMediaIds.has(String(item.id)),
        )
        .map((item) => ({
          id: `existing-stock-${item.id}`,
          preparedAt: null,
          media: item,
          bottleCount: item.availableBottles,
          technician: "Previous stock",
        }));
      setRows([...savedRows, ...existingStockRows]);
    }
  }
  useEffect(() => {
    load();
  }, []);
  async function save(e) {
    e.preventDefault();
    setError("");
    try {
      const amount = Number(bottleCount);
      try {
        const preparation = { bottleCount: amount, ph: Number(ph), agar: Number(agar), sterilizationMethod };
        if (editingId) await api.put(`/media-preparations/${editingId}`, preparation);
        else await api.post("/media-preparations", { mediaId, ...preparation });
      } catch (requestError) {
        if (requestError.response?.status !== 404 && requestError.response?.status !== 405) {
          throw requestError;
        }

        // Compatibility for an already-running backend from before preparation
        // history was introduced. It still updates the composition's stock.
        if (editingId) throw requestError;
        const composition = compositions.find(
          (item) => String(item.id) === String(mediaId),
        );
        if (!composition) throw requestError;
        const updatedComposition = {
          ...composition,
          availableBottles: composition.availableBottles + amount,
        };
        await api.patch(`/media/${mediaId}/stock`, {
          availableBottles: updatedComposition.availableBottles,
        });
        const savedRows = JSON.parse(
          localStorage.getItem(legacyHistoryKey) || "[]",
        );
        localStorage.setItem(
          legacyHistoryKey,
          JSON.stringify([
            {
              id: `legacy-${Date.now()}`,
              preparedAt: new Date().toISOString(),
              media: updatedComposition,
              bottleCount: amount,
              ph: Number(ph),
              agar: Number(agar),
              sterilizationMethod,
              technician: user.username,
            },
            ...savedRows,
          ]),
        );
      }
      setBottleCount("");
      setPh(""); setAgar(""); setSterilizationMethod("");
      setMediaId(""); setEditingId(null);
      await load();
    } catch (e) {
      setError(
        e.response?.data?.message || "Could not save media preparation.",
      );
    }
  }
  const selected = compositions.find(
    (item) => String(item.id) === String(mediaId),
  );
  function selectComposition(value) {
    setMediaId(value);
    const composition = compositions.find((item) => String(item.id) === String(value));
    setPh(composition?.ph ?? "");
    setAgar(composition?.agar ?? "");
  }
  function editPreparation(item) {
    setEditingId(item.id); setMediaId(String(item.media.id));
    setBottleCount(String(item.bottleCount));
    setPh(item.ph ?? item.media.ph ?? ""); setAgar(item.agar ?? item.media.agar ?? "");
    setSterilizationMethod(item.sterilizationMethod || ""); setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function cancelEdit() {
    setEditingId(null); setMediaId(""); setBottleCount(""); setPh(""); setAgar("");
    setSterilizationMethod(""); setError("");
  }
  return (
    <>
      <header>
        <p className="eyebrow">MEDIA PREPARATION</p>
        <h1>Register prepared media bottles</h1>
        <p className="muted">
          Select an approved composition and record the number of bottles
          prepared. Stock updates automatically.
        </p>
      </header>
      <section className="split workflow-layout">
        <form className="panel form media-preparation-form" onSubmit={save}>
          <h2>{editingId ? "Edit media preparation" : "New preparation"}</h2>
          {error && <div className="error">{error}</div>}
          <label>
            Media composition
            <select
              value={mediaId}
              onChange={(e) => selectComposition(e.target.value)}
              disabled={Boolean(editingId)}
              required
            >
              <option value="">Select composition…</option>
              {compositions.map((item) => (
                <option value={item.id} key={item.id}>
                  {item.code} — {item.basalMedia}
                </option>
              ))}
            </select>
          </label>
          {selected && (
            <div className="stock-status">
              Currently available: <b>{selected.availableBottles}</b> bottles
            </div>
          )}
          <label>
            Batch pH
            <input type="number" min="0" step="0.01" value={ph} onChange={(e) => setPh(e.target.value)} required />
          </label>
          <label>
            Batch agar (g/L)
            <input type="number" min="0" step="0.01" value={agar} onChange={(e) => setAgar(e.target.value)} required />
          </label>
          <label>
            Sterilization method
            <select value={sterilizationMethod} onChange={(e) => setSterilizationMethod(e.target.value)} required>
              <option value="" disabled>Select sterilization method...</option>
              <option value="AUTOCLAVE">Autoclave</option>
              <option value="CSUP">CSUP</option>
            </select>
          </label>
          <label>
            Number of prepared bottles
            <input
              type="number"
              min="1"
              value={bottleCount}
              onChange={(e) => setBottleCount(e.target.value)}
              disabled={Boolean(editingId) && user.role !== "ADMIN"}
              required
            />
          </label>
          {editingId && user.role !== "ADMIN" && <small className="authority-note">Only an administrator can change the bottle quantity of a past preparation.</small>}
          <button>{editingId ? "Update preparation" : "Add bottles to inventory"}</button>
          {editingId && <button type="button" className="secondary" onClick={cancelEdit}>Cancel editing</button>}
        </form>
        <section className="panel table-wrap media-preparation-list">
          <table>
            <thead>
              <tr>
                <th>Date / time</th>
                <th>Media</th>
                <th>Bottles added</th>
                <th>pH / agar</th>
                <th>Sterilization</th>
                {user.role === "ADMIN" && <th>Prepared by</th>}
                <th>Current stock</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.id}>
                  <td>
                    {item.preparedAt
                      ? new Date(item.preparedAt).toLocaleString()
                      : "Before history tracking"}
                  </td>
                  <td>
                    <b>{item.media.code}</b>
                    <small className="cell-sub">{item.media.basalMedia}</small>
                  </td>
                  <td>+{item.bottleCount}</td>
                  <td>{item.ph ?? item.media.ph ?? "—"} / {item.agar ?? item.media.agar ?? "—"} g/L</td>
                  <td>{item.sterilizationMethod === "AUTOCLAVE" ? "Autoclave" : item.sterilizationMethod === "CSUP" ? "CSUP" : "Not recorded"}</td>
                  {user.role === "ADMIN" && <td>{item.technician}</td>}
                  <td>{item.media.availableBottles}</td>
                  <td>{!String(item.id).startsWith("legacy-") && !String(item.id).startsWith("existing-stock-") ? <button type="button" className="secondary compact" onClick={() => editPreparation(item)}><Pencil size={14} /> Edit</button> : "—"}</td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan={user.role === "ADMIN" ? 8 : 7} className="empty">
                    No media preparations yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
      </section>
    </>
  );
}

function BottleBarcodeLabel({ item }) {
  const scanCode = `92${String(item.id).padStart(8, "0")}`;
  return (
    <div className="print-label initiation-print-label" data-subculture-label-id={item.id}>
      <Barcode
        value={scanCode}
        format="CODE128C"
        width={1.5}
        height={18}
        margin={0}
        displayValue={false}
      />
      <strong>{item.barcode}</strong>
    </div>
  );
}

function InitiationBarcodeLabel({ item }) {
  const scanCode = `91${String(item.id).padStart(8, "0")}`;
  return (
    <div className="print-label initiation-print-label" data-initiation-label-id={item.id}>
      <Barcode
        value={scanCode}
        format="CODE128C"
        width={1.5}
        height={18}
        margin={0}
        displayValue={false}
      />
      <strong>{item.barcode}</strong>
    </div>
  );
}

function SubculturePage({ user, sessionLaminaFlow }) {
  const blankLine = {
    bottleCount: 1,
    plantsPerBottle: 1,
    cultureType: "MULTIPLY",
  };
  const clearedLine = {
    bottleCount: "",
    plantsPerBottle: "",
    cultureType: "",
  };
  const [rows, setRows] = useState([]),
    [options, setOptions] = useState({ media: [], bottles: [] }),
    [parentBarcode, setParentBarcode] = useState(""),
    [scan, setScan] = useState(null),
    [mediaId, setMediaId] = useState(""),
    [lines, setLines] = useState([{ ...blankLine }]),
    [laminaFlow, setLaminaFlow] = useState(sessionLaminaFlow || "LF-01"),
    [technicianUsername, setTechnicianUsername] = useState(user.username),
    [staff, setStaff] = useState([]),
    [selectedForPrint, setSelectedForPrint] = useState([]),
    [created, setCreated] = useState([]),
    [saving, setSaving] = useState(false),
    [error, setError] = useState("");
  const parentBarcodeInput = useRef(null);
  function resetSubcultureEntry(preserveCreated = false) {
    setScan(null);
    setParentBarcode("");
    setMediaId("");
    setLines([{ ...clearedLine }]);
    setSelectedForPrint([]);
    setError("");
    if (!preserveCreated) setCreated([]);
    window.setTimeout(() => parentBarcodeInput.current?.focus(), 0);
  }
  async function load() {
    const [records, lookups] = await Promise.all([
      api.get("/subcultures"),
      api.get("/workflow/options"),
    ]);
    setRows(records.data);
    setOptions(lookups.data);
  }
  useEffect(() => {
    load();
    if (user.role === "ADMIN") {
      api.get("/users").then((response) =>
        setStaff(response.data.filter((person) => person.active)),
      );
    }
  }, []);
  async function scanParent() {
    setError("");
    try {
      const { data } = await api.get(
        `/workflow/scan/${encodeURIComponent(parentBarcode)}`,
      );
      if (data.status !== "ACTIVE") {
        const statusMessages = {
          USED: "This bottle has already been subcultured. Scan an active bottle instead.",
          DISCARDED: "This bottle has already been discarded and cannot be subcultured.",
          EXITED: "The plants in this bottle have already left the laboratory.",
        };
        const message =
          statusMessages[data.status] || "This bottle is not available for subculture.";
        window.alert(message);
        resetSubcultureEntry();
        return;
      }
      setScan(data);
    } catch (e) {
      const message = e.response?.data?.message || "Barcode not found.";
      window.alert(message);
      resetSubcultureEntry();
    }
  }
  function updateLine(index, key, value) {
    setLines(
      lines.map((line, i) => (i === index ? { ...line, [key]: value } : line)),
    );
  }
  async function save(e) {
    e.preventDefault();
    if (saving) return;
    setError("");
    setSaving(true);
    try {
      const { data } = await api.post("/subcultures", {
        parentBarcode,
        mediaId,
        lines,
        laminaFlow,
        technicianUsername,
      });
      setCreated(data);
      resetSubcultureEntry(true);
      await load();
    } catch (e) {
      const message =
        e.response?.data?.message ||
        (e.response
          ? "Could not create subculture bottles. Please check the selected bottle and media stock."
          : "Could not reach the server. Check the internet connection and try again.");
      setError(message);
      window.alert(message);
      resetSubcultureEntry();
    } finally {
      setSaving(false);
    }
  }
  const selectedMedia = options.media.find(
    (item) => String(item.id) === String(mediaId),
  );
  const totalBottles = lines.reduce(
    (sum, line) => sum + Number(line.bottleCount || 0),
    0,
  );
  const admin = user.role === "ADMIN";
  const printRows = created.length
    ? created
    : rows.filter((item) => selectedForPrint.includes(item.id));
  function togglePrint(id) {
    setCreated([]);
    setSelectedForPrint((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id],
    );
  }
  function printOne(id) {
    setCreated([]);
    setSelectedForPrint([id]);
    window.setTimeout(() => printSubcultureLabels([id]), 100);
  }
  function printSubcultureLabels(ids) {
    const labels = ids
      .map((id) => document.querySelector(`[data-subculture-label-id="${id}"]`)?.outerHTML)
      .filter(Boolean);
    if (!labels.length) return;
    const frame = document.createElement("iframe");
    frame.setAttribute("title", "Subculture barcode labels");
    frame.style.cssText = "position:fixed;width:0;height:0;border:0;right:0;bottom:0";
    document.body.appendChild(frame);
    const printDocument = frame.contentDocument;
    printDocument.open();
    printDocument.write(`<!doctype html><html><head><title></title><style>
      @page { size: 10mm 30mm; margin: 0; }
      html, body { width: 10mm; height: 30mm; margin: 0; padding: 0; background: #fff; }
      * { box-sizing: border-box; }
      .initiation-print-label { width: 30mm; height: 10mm; padding: .35mm .6mm; overflow: hidden; text-align: center; transform: translateX(10mm) rotate(90deg); transform-origin: top left; break-inside: avoid; page-break-inside: avoid; page-break-after: always; }
      .initiation-print-label:last-child { page-break-after: auto; }
      .initiation-print-label svg { display: block; width: 28.8mm; max-width: 28.8mm; height: 6.2mm; margin: 0 auto; }
      .initiation-print-label strong { display: block; overflow: hidden; margin: .2mm 0 0; font: 700 5pt/1 monospace; letter-spacing: -.15pt; white-space: nowrap; }
    </style></head><body>${labels.join("")}</body></html>`);
    printDocument.close();
    frame.contentWindow.focus();
    frame.contentWindow.print();
    setTimeout(() => frame.remove(), 1500);
  }
  return (
    <>
      <header>
        <p className="eyebrow">SUBCULTURE TRANSACTION</p>
        <h1>Register subculture</h1>
        <p className="muted">
          Scan a parent bottle, split the work into bottle groups, then generate
          every child barcode.
        </p>
      </header>
      <section className="panel subculture-form">
        <div className="session-fields">
          {admin ? <><label>Technician<select value={technicianUsername} onChange={(e) => setTechnicianUsername(e.target.value)}>{staff.map((person) => <option value={person.username} key={person.id}>{person.fullName} ({person.username})</option>)}</select></label><label>Lamina flow<select value={laminaFlow} onChange={(e) => setLaminaFlow(e.target.value)}><option>LF-01</option><option>LF-02</option><option>LF-03</option><option>LF-04</option></select></label></> : <div className="stock-status">Session: <b>{user.fullName}</b> · Lamina flow: <b>{laminaFlow}</b></div>}
        </div>
        <div className="scan-row">
          <label>
            Scan/read parent barcode
            <input
              ref={parentBarcodeInput}
              list="active-barcodes"
              value={parentBarcode}
              onChange={(e) => {
                setParentBarcode(e.target.value);
                setScan(null);
                setError("");
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  scanParent();
                }
              }}
              autoFocus
            />
          </label>
          <datalist id="active-barcodes">
            {options.bottles.map((item) => (
              <option value={item.value} key={item.value}>
                {item.label}
              </option>
            ))}
          </datalist>
          <button type="button" onClick={scanParent}>
            Detect barcode
          </button>
        </div>
        {error && <div className="error">{error}</div>}
        {scan && (
          <div className="scan-result">
            <span>
              <small>PLANT</small>
              <b>
                {scan.plantCode} — {scan.plantName}
              </b>
            </span>
            <span>
              <small>CURRENT CYCLE</small>
              <b>{scan.cycle}</b>
            </span>
            <span>
              <small>NEXT CYCLE</small>
              <b>{scan.nextCycle}</b>
            </span>
            <span>
              <small>YEAR / WEEK</small>
              <b>
                {scan.currentYear} / {String(scan.currentWeek).padStart(2, "0")}
              </b>
            </span>
          </div>
        )}
        <form onSubmit={save}>
          <div className="subculture-meta">
            <label>
              Subculture media
              <select
                value={mediaId}
                onChange={(e) => setMediaId(e.target.value)}
                required
              >
                <option value="">Select media…</option>
                {options.media.map((item) => (
                  <option value={item.id} key={item.id}>
                    {item.label} — {item.availableBottles} bottles available
                  </option>
                ))}
              </select>
            </label>
            <div
              className={`stock-status ${selectedMedia && selectedMedia.availableBottles < totalBottles ? "low" : ""}`}
            >
              Available: <b>{selectedMedia?.availableBottles ?? "—"}</b> ·
              Required: <b>{totalBottles}</b>
            </div>
          </div>
          <div className="culture-lines">
            <div className="line-head">
              <h2>Bottle groups</h2>
              <button
                type="button"
                className="secondary compact"
                onClick={() => setLines([...lines, { ...blankLine }])}
              >
                + Add line
              </button>
            </div>
            {lines.map((line, index) => (
              <div className="culture-line" key={index}>
                <label>
                  Number of bottles
                  <input
                    type="number"
                    min="1"
                    value={line.bottleCount}
                    onChange={(e) =>
                      updateLine(index, "bottleCount", e.target.value)
                    }
                    required
                  />
                </label>
                <label>
                  Plants / clumps per bottle
                  <input
                    type="number"
                    min="1"
                    value={line.plantsPerBottle}
                    onChange={(e) =>
                      updateLine(index, "plantsPerBottle", e.target.value)
                    }
                    required
                  />
                </label>
                <label>
                  Culture type
                  <select
                    value={line.cultureType}
                    onChange={(e) =>
                      updateLine(index, "cultureType", e.target.value)
                    }
                    required
                  >
                    <option value="">Select culture type…</option>
                    <option value="MULTIPLY">Multiply</option>
                    <option value="ROOTING">Rooting</option>
                  </select>
                </label>
                {lines.length > 1 && (
                  <button
                    type="button"
                    className="icon danger"
                    onClick={() =>
                      setLines(lines.filter((_, i) => i !== index))
                    }
                  >
                    <Trash2 size={17} />
                  </button>
                )}
              </div>
            ))}
          </div>
          <button disabled={!scan || !mediaId || saving}>
            {saving
              ? "Creating bottles…"
              : `Create ${totalBottles} bottles & generate barcodes`}
          </button>
        </form>
        {created.length > 0 && (
          <div className="print-panel">
            <div>
              <b>{created.length} barcodes created</b>
              <p>Print now or reopen this recent batch later.</p>
            </div>
            <button onClick={() => printSubcultureLabels(created.map((item) => item.id))}>Print all barcodes</button>
            <div className="barcode-sheet">
              {printRows.map((item) => (
                <BottleBarcodeLabel item={item} key={item.id} />
              ))}
            </div>
          </div>
        )}
        {!created.length && selectedForPrint.length > 0 && (
          <div className="barcode-sheet">{printRows.map((item) => <BottleBarcodeLabel item={item} key={item.id} />)}</div>
        )}
      </section>
      <section className="panel table-wrap recent-subcultures">
        <div className="line-head">
          <div>
            <p className="eyebrow">RECENTLY ADDED</p>
            <h2>{admin ? "All subculture bottles" : "Your recent subculture bottles"}</h2>
          </div>
          <button
            type="button"
            disabled={!selectedForPrint.length}
            onClick={() => printSubcultureLabels(selectedForPrint)}
          >
            Print selected ({selectedForPrint.length})
          </button>
        </div>
        <table>
          <thead><tr><th>Print</th><th>Barcode</th><th>Plant</th><th>Cycle / week</th><th>Type</th>{admin && <><th>Technician</th><th>Lamina flow</th></>}<th>Status</th></tr></thead>
          <tbody>{rows.map((item) => <tr key={item.id}><td><div className="print-actions"><input type="checkbox" aria-label={`Select ${item.barcode} for printing`} checked={selectedForPrint.includes(item.id)} onChange={() => togglePrint(item.id)}/><button type="button" className="secondary compact" onClick={() => printOne(item.id)}>Print one</button></div></td><td><b>{item.barcode}</b></td><td>{item.parent.plant.name}</td><td>C{item.cycle} · W{item.subcultureWeek}</td><td>{item.rooting ? "Rooting" : "Multiply"}</td>{admin && <><td>{item.technician}</td><td>{item.laminaFlow || "—"}</td></>}<td><span className="badge">{item.status}</span></td></tr>)}</tbody>
        </table>
      </section>
    </>
  );
}

function DiscardPage({ user }) {
  const [rows, setRows] = useState([]),
    [options, setOptions] = useState({ bottles: [] }),
    [barcode, setBarcode] = useState(""),
    [selectedReasons, setSelectedReasons] = useState([]),
    [otherReason, setOtherReason] = useState(""),
    [scan, setScan] = useState(null),
    [error, setError] = useState("");
  const discardBarcodeInput = useRef(null);
  function resetDiscardEntry() {
    setBarcode("");
    setSelectedReasons([]);
    setOtherReason("");
    setScan(null);
    setError("");
    window.setTimeout(() => discardBarcodeInput.current?.focus(), 0);
  }
  async function load() {
    const [records, lookups] = await Promise.all([
      api.get("/discards"),
      api.get("/workflow/options"),
    ]);
    setRows(records.data);
    setOptions(lookups.data);
  }
  useEffect(() => {
    load();
  }, []);
  async function detect() {
    setError("");
    try {
      const { data } = await api.get(
        `/workflow/scan/${encodeURIComponent(barcode)}`,
      );
      if (data.status !== "ACTIVE") {
        const statusMessages = {
          USED: "This bottle has already been subcultured and cannot be discarded as an active bottle.",
          DISCARDED: "This bottle has already been discarded.",
          EXITED: "The plants in this bottle have already left the laboratory.",
        };
        window.alert(
          statusMessages[data.status] || "This bottle is not available for discard.",
        );
        resetDiscardEntry();
        return;
      }
      setScan(data);
    } catch (e) {
      window.alert(e.response?.data?.message || "Barcode not found.");
      resetDiscardEntry();
    }
  }
  async function save(e) {
    e.preventDefault();
    setError("");
    const reason = selectedReasons
      .map((item) => (item === "Other" ? otherReason.trim() : item))
      .filter(Boolean)
      .join(", ");
    if (!reason) {
      setError("Select at least one discard reason.");
      return;
    }
    try {
      await api.post("/discards", { barcode, reason });
      resetDiscardEntry();
      await load();
    } catch (e) {
      window.alert(e.response?.data?.message || "Could not record discard.");
      resetDiscardEntry();
    }
  }
  const admin = user.role === "ADMIN";
  function toggleReason(reason) {
    setSelectedReasons((current) =>
      current.includes(reason)
        ? current.filter((item) => item !== reason)
        : [...current, reason],
    );
  }
  return (
    <>
      <header>
        <p className="eyebrow">DISCARD TRANSACTION</p>
        <h1>Record discarded bottle</h1>
        <p className="muted">
          Scan the bottle, verify its source, then record why it was discarded.
          Its plants leave active inventory automatically.
        </p>
      </header>
      <section className="panel subculture-form">
        <div className="scan-row">
          <label>
            Scan/read bottle barcode
            <input
              ref={discardBarcodeInput}
              list="discard-barcodes"
              value={barcode}
              onChange={(e) => {
                setBarcode(e.target.value);
                setScan(null);
                setError("");
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  detect();
                }
              }}
              autoFocus
            />
          </label>
          <datalist id="discard-barcodes">
            {options.bottles.map((item) => (
              <option value={item.value} key={item.value}>
                {item.label}
              </option>
            ))}
          </datalist>
          <button type="button" onClick={detect}>
            Detect barcode
          </button>
        </div>
        {error && <div className="error">{error}</div>}
        {scan && (
          <div className="scan-result">
            <span>
              <small>PLANT</small>
              <b>
                {scan.plantCode} — {scan.plantName}
              </b>
            </span>
            <span>
              <small>SUBCULTURE TECHNICIAN</small>
              <b>{scan.technician}</b>
            </span>
            <span>
              <small>CYCLE</small>
              <b>{scan.cycle}</b>
            </span>
            <span>
              <small>PARENT WEEK</small>
              <b>{scan.parentWeek}</b>
            </span>
          </div>
        )}
        <form className="form discard-reason" onSubmit={save}>
          <fieldset className="access-picker">
            <legend>Discard reason</legend>
            <p>Select every reason that applies.</p>
            <div>
              {["Bacterial", "Fungal", "Mites", "Plant dead", "Other"].map(
                (reason) => (
                  <label className="access-option" key={reason}>
                    <input
                      type="checkbox"
                      checked={selectedReasons.includes(reason)}
                      onChange={() => toggleReason(reason)}
                    />
                    <span>{reason}</span>
                  </label>
                ),
              )}
            </div>
          </fieldset>
          {selectedReasons.includes("Other") && (
            <label>
              Other reason
              <textarea
                value={otherReason}
                onChange={(e) => setOtherReason(e.target.value)}
                placeholder="Enter the other discard reason"
                required
              />
            </label>
          )}
          <button disabled={!scan}>Confirm discard & reduce inventory</button>
        </form>
      </section>
      <section className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Barcode</th>
              <th>Plant</th>
              <th>Plants removed</th>
              <th>Cycle / week</th>
              <th>Reason</th>
              {admin && (
                <>
                  <th>Original technician</th>
                  <th>Discarded by</th>
                </>
              )}
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.id}>
                <td>
                  <b>{item.barcode}</b>
                </td>
                <td>
                  {item.plantCode} — {item.plantName}
                </td>
                <td>{item.plantCount}</td>
                <td>
                  C{item.cycle} · W{item.cultureWeek}
                </td>
                <td>{item.reason}</td>
                {admin && (
                  <>
                    <td>{item.sourceTechnician || "—"}</td>
                    <td>{item.technician}</td>
                  </>
                )}
                <td>{item.discardedDate}</td>
              </tr>
            ))}
            {!rows.length && (
              <tr>
                <td colSpan={admin ? 8 : 6} className="empty">
                  No discarded bottles yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </>
  );
}

function SalesPage() {
  const [inventory, setInventory] = useState([]),
    [invoices, setInvoices] = useState([]),
    [quantities, setQuantities] = useState({}),
    [selectedCodes, setSelectedCodes] = useState([]),
    [search, setSearch] = useState(""),
    [customerName, setCustomerName] = useState(""),
    [customerContact, setCustomerContact] = useState(""),
    [lastInvoice, setLastInvoice] = useState(null),
    [error, setError] = useState("");

  async function load() {
    try {
      const [stock, sales] = await Promise.all([
        api.get("/sales/inventory"),
        api.get("/sales"),
      ]);
      setInventory(stock.data);
      setInvoices(sales.data);
    } catch (requestError) {
      setError(
        requestError.response?.status === 404 || requestError.response?.status === 405
          ? "Restart the backend once to activate Sales / POS."
          : requestError.response?.data?.message || "Could not load sales data.",
      );
    }
  }
  useEffect(() => {
    load();
  }, []);

  const selectedPlants = inventory.filter((item) =>
    selectedCodes.includes(item.plantCode),
  );
  const items = selectedPlants
    .map((item) => ({
      ...item,
      quantity: Number(quantities[item.plantCode] || 0),
    }))
    .filter((item) => item.quantity > 0);
  const searchTerm = search.trim().toLowerCase();
  const searchResults = searchTerm
    ? inventory.filter(
        (item) =>
          item.plantCode.toLowerCase().includes(searchTerm) ||
          item.plantName.toLowerCase().includes(searchTerm),
      )
    : [];
  const total = items.reduce(
    (sum, item) => sum + item.quantity * item.unitPrice,
    0,
  );

  async function completeSale(event) {
    event.preventDefault();
    setError("");
    if (!items.length) {
      setError("Add at least one plant quantity to the sale.");
      return;
    }
    try {
      const { data } = await api.post("/sales", {
        customerName,
        customerContact,
        items: items.map((item) => ({
          plantCode: item.plantCode,
          quantity: item.quantity,
        })),
      });
      setLastInvoice(data);
      setCustomerName("");
      setCustomerContact("");
      setQuantities({});
      setSelectedCodes([]);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not complete sale.");
    }
  }

  return (
    <>
      <header className="no-print">
        <p className="eyebrow">ROOTED PLANT SALES</p>
        <h1>Sales / POS</h1>
        <p className="muted">
          Sell up to 80% of available rooted plants and print the customer invoice.
        </p>
      </header>
      {error && <div className="error no-print">{error}</div>}
      <form className="panel pos-panel no-print" onSubmit={completeSale}>
        <div className="pos-customer">
          <label>Customer name<input value={customerName} onChange={(event) => setCustomerName(event.target.value)} required /></label>
          <label>Contact / phone<input value={customerContact} onChange={(event) => setCustomerContact(event.target.value)} /></label>
        </div>
        <div className="plant-search">
          <label>
            Search rooted plants by code or name
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Example: PHYM or Philodendron" />
          </label>
          {searchTerm && (
            <div className="plant-search-results">
              {searchResults.map((item) => (
                <div key={item.plantCode}>
                  <span><b>{item.plantCode}</b><small>{item.plantName} · {item.sellableQuantity} sellable</small></span>
                  <button type="button" className="secondary compact" disabled={selectedCodes.includes(item.plantCode) || item.sellableQuantity < 1} onClick={() => { setSelectedCodes([...selectedCodes, item.plantCode]); setQuantities({...quantities, [item.plantCode]: 1}); setSearch(""); }}>Add</button>
                </div>
              ))}
              {!searchResults.length && <p>No matching rooted plants.</p>}
            </div>
          )}
        </div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Plant</th><th>Total rooted</th><th>Sellable 80%</th><th>Unit price</th><th>Sale quantity</th><th>Subtotal</th><th></th></tr></thead>
            <tbody>
              {selectedPlants.map((item) => {
                const quantity = Number(quantities[item.plantCode] || 0);
                return <tr key={item.plantCode}><td><b>{item.plantCode}</b><small className="cell-sub">{item.plantName}</small></td><td>{item.rootedQuantity}</td><td><span className="badge">{item.sellableQuantity}</span></td><td>LKR {item.unitPrice.toFixed(2)}</td><td><input className="pos-quantity" type="number" min="1" max={item.sellableQuantity} value={quantities[item.plantCode] || ""} onChange={(event) => setQuantities({...quantities, [item.plantCode]: event.target.value})} /></td><td>LKR {(quantity * item.unitPrice).toFixed(2)}</td><td><button type="button" className="icon danger" aria-label={`Remove ${item.plantCode}`} onClick={() => { setSelectedCodes(selectedCodes.filter((code) => code !== item.plantCode)); const next = {...quantities}; delete next[item.plantCode]; setQuantities(next); }}><Trash2 size={16}/></button></td></tr>;
              })}
              {!selectedPlants.length && <tr><td colSpan="7" className="empty">Search above and add plants to this sale.</td></tr>}
            </tbody>
          </table>
        </div>
        <div className="pos-total"><span>Invoice total</span><strong>LKR {total.toFixed(2)}</strong><button>Complete sale</button></div>
      </form>
      {lastInvoice && (
        <section className="panel invoice" id="sale-invoice">
          <div className="invoice-head"><div><p className="eyebrow">NATURAL FOLIAGE TC LAB</p><h2>Sales Invoice</h2></div><button className="no-print" onClick={() => window.print()}>Print invoice</button></div>
          <div className="invoice-meta"><span><small>INVOICE</small><b>{lastInvoice.invoiceNumber}</b></span><span><small>CUSTOMER</small><b>{lastInvoice.customerName}</b></span><span><small>DATE</small><b>{new Date(lastInvoice.soldAt).toLocaleString()}</b></span></div>
          <table><thead><tr><th>Plant</th><th>Quantity</th><th>Unit price</th><th>Total</th></tr></thead><tbody>{lastInvoice.items.map((item) => <tr key={item.id}><td>{item.plantCode} — {item.plantName}</td><td>{item.quantity}</td><td>LKR {item.unitPrice.toFixed(2)}</td><td>LKR {item.lineTotal.toFixed(2)}</td></tr>)}</tbody></table>
          <div className="invoice-grand-total">Total: LKR {lastInvoice.totalAmount.toFixed(2)}</div>
        </section>
      )}
      <section className="panel table-wrap no-print"><h2>Recent invoices</h2><table><thead><tr><th>Invoice</th><th>Date</th><th>Customer</th><th>Sold by</th><th>Total</th></tr></thead><tbody>{invoices.map((item) => <tr key={item.id}><td><b>{item.invoiceNumber}</b></td><td>{new Date(item.soldAt).toLocaleString()}</td><td>{item.customerName}</td><td>{item.soldBy}</td><td>LKR {item.totalAmount.toFixed(2)}</td></tr>)}{!invoices.length && <tr><td colSpan="5" className="empty">No invoices yet.</td></tr>}</tbody></table></section>
    </>
  );
}

function PlantExitPage({ user }) {
  const [rows,setRows]=useState([]),[options,setOptions]=useState({bottles:[]}),[barcode,setBarcode]=useState(''),[scan,setScan]=useState(null),[quantity,setQuantity]=useState(''),[reference,setReference]=useState(''),[error,setError]=useState('');
  async function load(){const [exits,lookups]=await Promise.all([api.get('/plant-exits'),api.get('/workflow/options')]);setRows(exits.data);setOptions(lookups.data)}
  useEffect(()=>{load()},[]);
  async function detect(){setError('');try{const {data}=await api.get(`/workflow/scan/${encodeURIComponent(barcode)}`);setScan(data);setQuantity(data.plantCount)}catch(e){setScan(null);setError(e.response?.data?.message||'Barcode not found.')}}
  async function save(e){e.preventDefault();setError('');try{await api.post('/plant-exits',{barcode,quantity:Number(quantity),destination:'HARDENING',reference});setBarcode('');setScan(null);setQuantity('');setReference('');await load()}catch(e){setError(e.response?.data?.message||'Could not record hardening exit.')}}
  const admin=user.role==='ADMIN';
  return <><header><p className="eyebrow">HARDENING</p><h1>Plants sent for hardening</h1><p className="muted">Record healthy plants moved from the laboratory to hardening. Customer sales are handled only through Sales / POS.</p></header><section className="split workflow-layout"><form className="panel form" onSubmit={save}><h2>Release to hardening</h2><label>Scan/read subculture barcode<input list="exit-barcodes" value={barcode} onChange={e=>setBarcode(e.target.value)} onKeyDown={e=>{if(e.key==='Enter'){e.preventDefault();detect()}}} required/></label><datalist id="exit-barcodes">{options.bottles.filter(item=>item.label.includes('Subculture')).map(item=><option value={item.value} key={item.value}>{item.label}</option>)}</datalist><button type="button" className="secondary" onClick={detect}>Detect barcode</button>{error&&<div className="error">{error}</div>}{scan&&<><div className="stock-status"><b>{scan.plantCode} — {scan.plantName}</b><br/>Available in bottle: {scan.plantCount} · {scan.rooting?'Rooting':'Multiply'}</div><label>Quantity sent to hardening<input type="number" min="1" max={scan.plantCount} value={quantity} onChange={e=>setQuantity(e.target.value)} required/></label><label>Hardening location / reference<input value={reference} onChange={e=>setReference(e.target.value)} placeholder="Optional reference"/></label><button>Confirm hardening exit</button></>}</form><section className="panel table-wrap"><table><thead><tr><th>Date / time</th><th>Plant</th><th>Barcode</th><th>Quantity</th><th>Reference</th>{admin&&<th>Released by</th>}</tr></thead><tbody>{rows.filter(item=>item.destination==='HARDENING').map(item=><tr key={item.id}><td>{new Date(item.exitedAt).toLocaleString()}</td><td>{item.plantCode} — {item.plantName}</td><td><b>{item.barcode}</b></td><td>{item.quantity}</td><td>{item.reference||'—'}</td>{admin&&<td>{item.technician}</td>}</tr>)}{!rows.some(item=>item.destination==='HARDENING')&&<tr><td colSpan={admin?6:5} className="empty">No hardening exits yet.</td></tr>}</tbody></table></section></section></>
}

function WorkflowPage({ type, user }) {
  const paths = {
    "Plant Initiation": "/mother-bottles",
    Subcultures: "/subcultures",
    Discards: "/discards",
  };
  const initial =
    type === "Plant Initiation"
      ? { plantId: "", mediaId: "", plantCount: "", cycle: 0, laminaFlow: "" }
      : type === "Subcultures"
        ? {
            barcode: "",
            parentId: "",
            mediaId: "",
            plantCount: "",
            subcultureWeek: "",
            rooting: false,
            laminaFlow: "",
          }
        : { barcode: "", reason: "" };
  const [rows, setRows] = useState([]),
    [options, setOptions] = useState({
      plants: [],
      media: [],
      mothers: [],
      bottles: [],
    }),
    [form, setForm] = useState(initial),
    [nextBarcode, setNextBarcode] = useState("Select a plant to generate"),
    [selectedForPrint, setSelectedForPrint] = useState([]),
    [error, setError] = useState("");
  async function load() {
    const [records, lookups] = await Promise.all([
      api.get(paths[type]),
      api.get("/workflow/options"),
    ]);
    setRows(records.data);
    setOptions(lookups.data);
  }
  useEffect(() => {
    load();
  }, [type]);
  useEffect(() => {
    if (type === "Plant Initiation" && form.plantId)
      api
        .get(`/mother-bottles/next-barcode?plantId=${form.plantId}`)
        .then((r) => setNextBarcode(r.data));
  }, [type, form.plantId]);
  async function save(e) {
    e.preventDefault();
    setError("");
    try {
      const { data } = await api.post(paths[type], form);
      if (type === "Plant Initiation") setSelectedForPrint([data.id]);
      setForm(initial);
      await load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not save this entry.");
    }
  }
  async function removeInitiation(item) {
    if (!confirm(`Delete plant initiation ${item.barcode}?`)) return;
    setError("");
    try {
      await api.delete(`/mother-bottles/${item.id}`);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not delete this plant initiation.");
    }
  }
  function toggleInitiationPrint(id) {
    setSelectedForPrint((selected) =>
      selected.includes(id) ? selected.filter((itemId) => itemId !== id) : [...selected, id],
    );
  }
  function printInitiationLabels(ids) {
    const labels = ids
      .map((id) => document.querySelector(`[data-initiation-label-id="${id}"]`)?.outerHTML)
      .filter(Boolean);
    if (!labels.length) return;
    const frame = document.createElement("iframe");
    frame.setAttribute("title", "Plant initiation barcode labels");
    frame.style.cssText = "position:fixed;width:0;height:0;border:0;right:0;bottom:0";
    document.body.appendChild(frame);
    const printDocument = frame.contentDocument;
    printDocument.open();
    printDocument.write(`<!doctype html><html><head><title></title><style>
      @page { size: 10mm 30mm; margin: 0; }
      html, body { width: 10mm; height: 30mm; margin: 0; padding: 0; background: #fff; }
      * { box-sizing: border-box; }
      .initiation-print-label { width: 30mm; height: 10mm; padding: .35mm .6mm; overflow: hidden; text-align: center; transform: translateX(10mm) rotate(90deg); transform-origin: top left; break-inside: avoid; page-break-inside: avoid; page-break-after: always; }
      .initiation-print-label:last-child { page-break-after: auto; }
      .initiation-print-label svg { display: block; width: 28.8mm; max-width: 28.8mm; height: 6.2mm; margin: 0 auto; }
      .initiation-print-label strong { display: block; overflow: hidden; margin: .2mm 0 0; font: 700 5pt/1 monospace; letter-spacing: -.15pt; white-space: nowrap; }
    </style></head><body>${labels.join("")}</body></html>`);
    printDocument.close();
    frame.contentWindow.focus();
    frame.contentWindow.print();
    setTimeout(() => frame.remove(), 1500);
  }
  function printInitiation(id) {
    setSelectedForPrint([id]);
    printInitiationLabels([id]);
  }
  const select = (key, label, items) => (
    <label>
      {label}
      <select
        value={form[key]}
        onChange={(e) => setForm({ ...form, [key]: e.target.value })}
        required
      >
        <option value="">Select…</option>
        {items.map((item) => (
          <option value={item.id ?? item.value} key={item.id ?? item.value}>
            {item.label}
          </option>
        ))}
      </select>
    </label>
  );
  return (
    <>
      <header>
        <p className="eyebrow">LAB WORKFLOW</p>
        <h1>{type}</h1>
        <p className="muted">
          Register laboratory work and keep every entry linked to the signed-in
          technician.
        </p>
      </header>
      <section className="split workflow-layout">
        <form className={`panel form ${type === "Plant Initiation" ? "plant-initiation-form" : ""}`} onSubmit={save}>
          <h2>
            {type === "Discards"
              ? "Record discard"
              : `Register ${type === "Plant Initiation" ? "plant initiation" : "subculture"}`}
          </h2>
          {error && <div className="error">{error}</div>}
          <label className={type !== "Subcultures" ? "hidden-field" : ""}>
            Barcode
            <input
              value={form.barcode || ""}
              onChange={(e) => setForm({ ...form, barcode: e.target.value })}
              required={type === "Subcultures"}
            />
          </label>
          {type === "Plant Initiation" && (
            <>
              {select("plantId", "Plant", options.plants)}
              <div className="barcode-preview">
                <small>AUTOMATIC BARCODE</small>
                <strong>{nextBarcode}</strong>
              </div>
              {select("mediaId", "Media composition", options.media)}
              <label>
                Number of plants
                <input
                  type="number"
                  min="1"
                  value={form.plantCount}
                  onChange={(e) =>
                    setForm({ ...form, plantCount: e.target.value })
                  }
                  required
                />
              </label>
              <label>
                Cycle
                <input
                  type="number"
                  min="0"
                  value={form.cycle}
                  onChange={(e) => setForm({ ...form, cycle: e.target.value })}
                  required
                />
              </label>
              <div className="barcode-preview">
                <small>AUTOMATIC CULTURE DATE · ISO YEAR / WEEK</small>
                <strong>
                  {currentIsoDate().date} · {currentIsoDate().year} / W
                  {String(currentIsoDate().week).padStart(2, "0")}
                </strong>
              </div>
              <label>
                Lamina flow
                <input
                  value={form.laminaFlow}
                  onChange={(e) =>
                    setForm({ ...form, laminaFlow: e.target.value })
                  }
                  placeholder="e.g. LF1"
                />
              </label>
            </>
          )}
          {type === "Subcultures" && (
            <>
              {select("parentId", "Active plant initiation", options.mothers)}
              {select("mediaId", "Media composition", options.media)}
              <label>
                Number of plants
                <input
                  type="number"
                  min="1"
                  value={form.plantCount}
                  onChange={(e) =>
                    setForm({ ...form, plantCount: e.target.value })
                  }
                  required
                />
              </label>
              <label>
                Subculture week
                <input
                  type="number"
                  min="1"
                  max="53"
                  value={form.subcultureWeek}
                  onChange={(e) =>
                    setForm({ ...form, subcultureWeek: e.target.value })
                  }
                  required
                />
              </label>
              <label>
                Lamina flow
                <input
                  value={form.laminaFlow}
                  onChange={(e) =>
                    setForm({ ...form, laminaFlow: e.target.value })
                  }
                  placeholder="e.g. LF1"
                />
              </label>
              <label className="checkbox-field">
                <input
                  type="checkbox"
                  checked={form.rooting}
                  onChange={(e) =>
                    setForm({ ...form, rooting: e.target.checked })
                  }
                />
                <span>Rooting culture</span>
              </label>
            </>
          )}
          {type === "Discards" && (
            <>
              {select("barcode", "Active bottle", options.bottles)}
              <label>
                Discard reason
                <textarea
                  value={form.reason}
                  onChange={(e) => setForm({ ...form, reason: e.target.value })}
                  placeholder="Bacterial, fungal, mites, plant dead, or other"
                  required
                />
              </label>
            </>
          )}
          <button>
            {type === "Discards" ? "Save discard" : "Register entry"}
          </button>
        </form>
        <section className={`panel table-wrap ${type === "Plant Initiation" ? "plant-initiation-list" : ""}`}>
          {type === "Plant Initiation" && (
            <div className="line-head initiation-print-toolbar">
              <div>
                <b>30 mm × 10 mm barcode labels</b>
                <p>Select past initiations or print one label directly.</p>
              </div>
              <button type="button" disabled={!selectedForPrint.length} onClick={() => printInitiationLabels(selectedForPrint)}>
                Print selected ({selectedForPrint.length})
              </button>
            </div>
          )}
          <table>
            <thead>
              <tr>
                {type === "Plant Initiation" && <th>Print</th>}
                <th>Barcode</th>
                <th>Plant / reason</th>
                <th>Technician</th>
                <th>Status / date</th>
                {type === "Plant Initiation" && user.role === "ADMIN" && <th>Action</th>}
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  {type === "Plant Initiation" && (
                    <td>
                      <div className="print-actions">
                        <input
                          type="checkbox"
                          aria-label={`Select ${r.barcode} for printing`}
                          checked={selectedForPrint.includes(r.id)}
                          onChange={() => toggleInitiationPrint(r.id)}
                        />
                        <button type="button" className="secondary compact" onClick={() => printInitiation(r.id)}>Print one</button>
                      </div>
                    </td>
                  )}
                  <td>
                    <b>{r.barcode}</b>
                  </td>
                  <td>
                    {r.plant?.name || r.parent?.plant?.name || r.reason || "—"}
                  </td>
                  <td>{r.technician}</td>
                  <td>
                    <span className="badge">
                      {type === "Plant Initiation"
                        ? ({ ACTIVE: "Active", USED: "Subcultured", DISCARDED: "Discarded" }[r.status] || r.status)
                        : r.status || r.discardedDate}
                    </span>
                  </td>
                  {type === "Plant Initiation" && user.role === "ADMIN" && (
                    <td><button type="button" className="icon danger" aria-label={`Delete ${r.barcode}`} onClick={() => removeInitiation(r)}><Trash2 size={16} /></button></td>
                  )}
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan={type === "Plant Initiation" ? (user.role === "ADMIN" ? 6 : 5) : 4} className="empty">
                    No records yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          {type === "Plant Initiation" && (
            <div className="initiation-label-library" aria-hidden="true">
              {rows.map((item) => <InitiationBarcodeLabel item={item} key={item.id} />)}
            </div>
          )}
        </section>
      </section>
    </>
  );
}

function StaffPage() {
  const initial = {
    employeeId: "",
    fullName: "",
    username: "",
    password: "",
    confirmPassword: "",
    role: "TECHNICIAN",
    labSection: "Subculture",
    active: true,
    permissions: ["ACCESS_DASHBOARD"],
  };
  const [staff, setStaff] = useState([]),
    [form, setForm] = useState(initial),
    [editingId, setEditingId] = useState(null),
    [error, setError] = useState("");
  const load = () => api.get("/users").then((r) => setStaff(r.data));
  useEffect(() => {
    load();
  }, []);
  async function save(e) {
    e.preventDefault();
    setError("");
    if (form.password !== form.confirmPassword) {
      setError("Password and confirm password do not match.");
      return;
    }
    if (!editingId && !form.password) {
      setError("Password is required.");
      return;
    }
    try {
      const { confirmPassword: _confirmPassword, ...payload } = form;
      if (editingId) await api.put(`/users/${editingId}`, payload);
      else await api.post("/users", payload);
      resetForm();
      await load();
    } catch (e) {
      setError(e.response?.data?.message || `Could not ${editingId ? "update" : "create"} staff account.`);
    }
  }
  function resetForm() {
    setForm({ ...initial, permissions: [...initial.permissions] });
    setEditingId(null);
    setError("");
  }
  function edit(person) {
    setEditingId(person.id);
    setForm({
      employeeId: person.employeeId || "",
      fullName: person.fullName || "",
      username: person.username || "",
      password: "",
      confirmPassword: "",
      role: person.role || "TECHNICIAN",
      labSection: person.labSection || "Subculture",
      active: person.active,
      permissions: [...(person.permissions || [])],
    });
    setError("");
  }
  async function remove(person) {
    if (!window.confirm(`Delete ${person.fullName}'s staff account?`)) return;
    try {
      await api.delete(`/users/${person.id}`);
      if (editingId === person.id) resetForm();
      await load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not delete staff account.");
    }
  }
  async function toggle(person) {
    try {
      await api.patch(`/users/${person.id}/status`, { active: !person.active });
      load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not change account status.");
    }
  }
  async function updateAccess(person, permission) {
    const current = person.permissions || [];
    const permissions = current.includes(permission)
      ? current.filter((item) => item !== permission)
      : [...current, permission];
    try {
      await api.patch(`/users/${person.id}/permissions`, { permissions });
      load();
    } catch (e) {
      setError(e.response?.data?.message || "Could not change user access.");
    }
  }
  async function resetAllData() {
    const confirmation = window.prompt(
      "This permanently deletes every record and staff account except the main admin. Type DELETE ALL LAB DATA to continue.",
    );
    if (confirmation !== "DELETE ALL LAB DATA") return;
    try {
      const { data } = await api.delete("/admin/data", {
        data: { confirmation },
      });
      window.alert(
        `Lab data cleared successfully. ${Object.values(data).reduce((sum, count) => sum + count, 0)} records removed.`,
      );
      setForm(initial);
      setError("");
      load();
    } catch (e) {
      window.alert(e.response?.data?.message || "Could not clear lab data.");
    }
  }
  function changeAccess(permission) {
    setForm({
      ...form,
      permissions: form.permissions.includes(permission)
        ? form.permissions.filter((item) => item !== permission)
        : [...form.permissions, permission],
    });
  }
  return (
    <>
      <header>
        <p className="eyebrow">ADMINISTRATION</p>
        <h1>Staff & access control</h1>
        <p className="muted">
          Create individual accounts and choose exactly which parts of the
          system they can use.
        </p>
      </header>
      <section className="split staff-layout">
        <form className="panel form staff-form-panel" onSubmit={save}>
          <h2>{editingId ? "Edit staff account" : "Create staff account"}</h2>
          {error && <div className="error">{error}</div>}
          <label>
            Employee ID
            <input
              value={form.employeeId}
              onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
              required
            />
          </label>
          <label>
            Full name
            <input
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
          </label>
          <label>
            Username
            <input
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </label>
          <label>
            Password {editingId && <small className="field-hint">(leave blank to keep current password)</small>}
            <input
              type="password"
              minLength="8"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required={!editingId}
            />
          </label>
          <label>
            Confirm password
            <input
              type="password"
              minLength="8"
              value={form.confirmPassword}
              onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              required={!editingId || Boolean(form.password)}
            />
          </label>
          <label>
            Role
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
            >
              <option value="TECHNICIAN">Technician</option>
              <option value="ADMIN">Administrator (full access)</option>
            </select>
          </label>
          <label>
            Lab section
            <select
              value={form.labSection}
              onChange={(e) => setForm({ ...form, labSection: e.target.value })}
            >
              <option>Subculture</option>
              <option>Media Preparation</option>
              <option>Mother Culture</option>
              <option>Rooting</option>
              <option>Administration</option>
            </select>
          </label>
          {form.role === "TECHNICIAN" && (
            <fieldset className="access-picker">
              <legend>System access</legend>
              <p>Select the tabs this user is allowed to open.</p>
              <div>
                {accessOptions.map(([label, permission]) => (
                  <label className="access-option" key={permission}>
                    <input
                      type="checkbox"
                      checked={form.permissions.includes(permission)}
                      onChange={() => changeAccess(permission)}
                    />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}
          <div className="staff-form-actions">
            <button>{editingId ? "Save changes" : "Create account"}</button>
            {editingId && <button type="button" className="secondary" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
        <section className="panel table-wrap staff-list-panel">
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Username</th>
                <th>Role / section</th>
                <th>Access</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {staff.map((person) => (
                <tr key={person.id}>
                  <td>
                    <b>{person.fullName}</b>
                    <small className="cell-sub">{person.employeeId}</small>
                  </td>
                  <td>{person.username}</td>
                  <td>
                    {person.role}
                    <small className="cell-sub">
                      {person.labSection || "—"}
                    </small>
                  </td>
                  <td>
                    <div className="permission-list">
                      {person.role === "ADMIN" ? (
                        <span className="permission-tag admin-access">
                          Full access
                        </span>
                      ) : (
                        accessOptions.map(([label, permission]) => (
                          <label className="permission-toggle" key={permission}>
                            <input
                              type="checkbox"
                              checked={(person.permissions || []).includes(
                                permission,
                              )}
                              onChange={() => updateAccess(person, permission)}
                            />
                            <span>{label}</span>
                          </label>
                        ))
                      )}
                    </div>
                  </td>
                  <td>
                    <span
                      className={`badge ${person.active ? "" : "inactive"}`}
                    >
                      {person.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td>
                    <div className="staff-row-actions">
                      <button type="button" className="icon edit" aria-label={`Edit ${person.fullName}`} title="Edit" onClick={() => edit(person)}><Pencil size={16} /></button>
                      {person.username.toLowerCase() !== "admin" && <button type="button" className="icon danger" aria-label={`Delete ${person.fullName}`} title="Delete" onClick={() => remove(person)}><Trash2 size={16} /></button>}
                      <button type="button" className="secondary compact" onClick={() => toggle(person)}>
                        {person.active ? "Deactivate" : "Activate"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </section>
      <section className="panel danger-zone">
        <div>
          <h2>Production data reset</h2>
          <p className="muted">Permanently delete every lab record and staff account except the main admin account.</p>
        </div>
        <button type="button" className="danger" onClick={resetAllData}>Clear all lab data</button>
      </section>
    </>
  );
}

function WorkspaceClock({ activePage, user }) {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return <section className="workspace-clock"><div><small>CURRENT SECTION</small><b>{activePage}</b><span>{user.fullName} · {user.role}</span></div><time dateTime={now.toISOString()}><small>LOCAL DATE & TIME</small><b>{new Intl.DateTimeFormat("en-LK", { dateStyle: "full", timeStyle: "medium" }).format(now)}</b></time></section>;
}

function App() {
  const token = sessionStorage.getItem("labAuth");
  if (token) api.defaults.headers.common.Authorization = `Basic ${token}`;
  const [user, setUser] = useState(null),
    [page, setPage] = useState(null),
    [loading, setLoading] = useState(!!token),
    [sessionLaminaFlow, setSessionLaminaFlow] = useState(
      sessionStorage.getItem("laminaFlow") || "",
    );
  useEffect(() => {
    if (token)
      api
        .get("/me")
        .then((r) => setUser(r.data))
        .catch(() => sessionStorage.removeItem("labAuth"))
        .finally(() => setLoading(false));
  }, []);
  if (loading) return <div className="loading">Loading…</div>;
  if (!user) return <Login onLogin={setUser} />;
  const allowed = new Set(user.permissions || []);
  if (
    user.role === "TECHNICIAN" &&
    allowed.has("ACCESS_SUBCULTURES") &&
    !sessionLaminaFlow
  ) {
    return (
      <LaminaFlowSetup
        user={user}
        onSelect={(value) => {
          sessionStorage.setItem("laminaFlow", value);
          setSessionLaminaFlow(value);
        }}
      />
    );
  }
  const visibleNav = nav.filter(
    ([label]) =>
      user.role === "ADMIN" ||
      (label !== "Staff" &&
        allowed.has(accessOptions.find((item) => item[0] === label)?.[1])),
  );
  const activePage =
    page && visibleNav.some(([label]) => label === page)
      ? page
      : visibleNav[0]?.[0];
  if (!activePage)
    return (
      <div className="loading">
        Your account has no system access. Ask an administrator to assign
        access.
      </div>
    );
  let content =
    activePage === "Dashboard" ? (
      <Dashboard user={user} />
    ) : activePage === "Analytics" ? (
      <AnalyticsPage />
    ) : activePage === "Staff" ? (
      <StaffPage />
    ) : activePage === "Media Compositions" ? (
      <MediaCompositionPage />
    ) : activePage === "Media Bottles" ? (
      <MediaBottlesPage user={user} />
    ) : activePage === "Subcultures" ? (
      <SubculturePage user={user} sessionLaminaFlow={sessionLaminaFlow} />
    ) : activePage === "Discards" ? (
      <DiscardPage user={user} />
    ) : activePage === "Plant Exit" ? (
      <PlantExitPage user={user} />
    ) : activePage === "Sales / POS" ? (
      <SalesPage />
    ) : configs[activePage] ? (
      <CrudPage type={activePage} user={user} />
    ) : (
      <WorkflowPage type={activePage} user={user} />
    );
  return (
    <div className="shell">
      <aside>
        <div className="logo">
          <Leaf />
          <div>
            <b>Natural Foliage</b>
            <span>LAB SYSTEM</span>
          </div>
        </div>
        <nav>
          {visibleNav.map(([label, Icon]) => (
            <button
              className={activePage === label ? "active" : ""}
              onClick={() => setPage(label)}
              key={label}
            >
              <Icon size={19} />
              {label}
            </button>
          ))}
        </nav>
        <div className="profile">
          <div className="avatar">{user.fullName[0]}</div>
          <div>
            <b>{user.fullName}</b>
            <span>{user.role}</span>
          </div>
          <button
            className="icon"
            onClick={() => {
              sessionStorage.clear();
              delete api.defaults.headers.common.Authorization;
              setUser(null);
            }}
          >
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <main className="content"><WorkspaceClock activePage={activePage} user={user} />{content}</main>
    </div>
  );
}
createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
